package com.cyzah.circular.core.network

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Handler
import java.util.Collections
import java.util.HashMap

/**
 * Runs in privileged SystemUI. Registration failures remain contained by NetworkSource startup
 * rollback.
 */
@SuppressLint("MissingPermission")
class WifiNetworkMonitor(
    private val connectivity: ConnectivityManager,
    private val host: Host
) {
    interface Host {
        fun running(): Boolean

        fun main(): Handler?

        fun refreshWifi(force: Boolean)

        fun recovered()

        fun changed()

        fun error(area: String?, error: Throwable?)

        fun recordFailure(phase: String?, error: Throwable?)

        fun cleanupComplete()
    }

    private val networks = HashMap<Network?, WifiInfo?>()
    private var callback: Callback? = null
    var isStarted: Boolean = false
        private set
    private var cleanupAttempt = 0
    private var cleanupRetry: Runnable? = null

    fun start() {
        if (this.isStarted) {
            return
        }
        if (this.callback != null) {
            stop()
            check(this.callback == null) { "Previous Wi-Fi callback cleanup is pending" }
        }
        val next = Callback()
        // A platform call may register the callback and then throw.
        this.callback = next
        try {
            connectivity.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build(),
                next,
                host.main()!!
            )
            this.isStarted = true
        } catch (error: Throwable) {
            stop()
            throw error
        }
    }

    fun stop() {
        // Revoke callback updates even if native cleanup cannot complete yet.
        this.isStarted = false
        this.networks.clear()
        if (this.callback != null) {
            try {
                this.connectivity.unregisterNetworkCallback(this.callback!!)
            } catch (error: Throwable) {
                // ConnectivityManager uses this exact result when the callback has no request.
                if (error !is IllegalArgumentException || "NetworkCallback was not registered" != error.message) {
                    host.recordFailure("callback-unregister", error)
                    scheduleCleanup()
                    return
                }
            }
            this.callback = null
            this.cleanupAttempt = 0
            if (this.cleanupRetry != null) {
                this.host.main()!!.removeCallbacks(this.cleanupRetry!!)
                this.cleanupRetry = null
            }
        }
    }

    fun ownsRegistration(): Boolean {
        return this.callback != null
    }

    private fun scheduleCleanup() {
        // Retain ownership after the finite retry budget; explicit stop/start may retry.
        if (this.cleanupRetry != null || this.cleanupAttempt >= 5) {
            return
        }
        val retired = this.callback
        val retry: Runnable = object : Runnable {
            override fun run() {
                if (cleanupRetry !== this) {
                    return
                }
                cleanupRetry = null
                if (callback == retired && !this@WifiNetworkMonitor.isStarted) {
                    stop()
                    if (callback == null) {
                        host.cleanupComplete()
                    }
                }
            }
        }
        this.cleanupRetry = retry
        this.host.main()!!.postDelayed(retry, 2000L shl this.cleanupAttempt++)
    }

    fun networks(): MutableMap<Network?, WifiInfo?> {
        return Collections.unmodifiableMap<Network?, WifiInfo?>(this.networks)
    }

    fun sampleCount(): Int {
        return this.networks.size
    }

    fun clear() {
        this.networks.clear()
    }

    private fun active(source: Callback?): Boolean {
        return this.isStarted && this.callback == source && this.host.running()
    }

    private inner class Callback : NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            this.post(object : Runnable {
                override fun run() {
                    applyCapabilities(capabilities, network)
                }
            })
        }

        override fun onLost(network: Network) {
            this.post(object : Runnable {
                override fun run() {
                    forget(network)
                }
            })
        }

        fun post(action: Runnable?) {
            host.main()!!.post(action!!)
        }

        fun applyCapabilities(
            capabilities: NetworkCapabilities,
            network: Network?
        ) {
            if (!active(this)) {
                return
            }
            try {
                if (capabilities.getTransportInfo() is WifiInfo) {
                    networks.put(network, capabilities.getTransportInfo() as WifiInfo?)
                } else {
                    networks.remove(network)
                }
                host.refreshWifi(false)
                host.recovered()
                host.changed()
            } catch (error: Throwable) {
                host.error("Wi-Fi capabilities", error)
            }
        }

        fun forget(network: Network?) {
            if (!active(this)) {
                return
            }
            networks.remove(network)
            try {
                host.refreshWifi(true)
                host.recovered()
                host.changed()
            } catch (error: Throwable) {
                host.error("Wi-Fi lost", error)
            }
        }
    }
}
