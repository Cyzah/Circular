package com.cyzah.circular.core.network

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import java.lang.reflect.Method

@SuppressLint("DiscouragedPrivateApi")
class PerformanceModeSource(
    context: Context,
    handler: Handler?,
    private val state: IconState,
    private val changed: Runnable
) {
    private var known = false
    private var nativeValue: Boolean? = null
    private val observeForUser: Method?
    private val observer: ContentObserver
    private val readForUser: Method?
    private var registered = false
    private val resolver: ContentResolver
    private var running = false
    private var source: String? = "pending"

    init {
        this.resolver = context.getContentResolver()
        this.observer = object : ContentObserver(handler) {
            // android.database.ContentObserver
            override fun onChange(z: Boolean) {
                if (this@PerformanceModeSource.running) {
                    this@PerformanceModeSource.refresh()
                }
            }
        }
        var reader: Method? = null
        var observerRegistration: Method? = null
        try {
            reader = Settings.System::class.java.getDeclaredMethod(
                "getIntForUser",
                ContentResolver::class.java,
                String::class.java,
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!
            )
            reader.setAccessible(true)
            observerRegistration = ContentResolver::class.java.getMethod(
                "registerContentObserver",
                Uri::class.java,
                Boolean::class.javaPrimitiveType!!,
                ContentObserver::class.java,
                Int::class.javaPrimitiveType!!
            )
            observerRegistration.setAccessible(true)
        } catch (e: ReflectiveOperationException) {
        } catch (e: RuntimeException) {
        }
        this.readForUser = reader
        this.observeForUser = observerRegistration
    }

    fun setNative(z: Boolean) {
        this.nativeValue = z
        apply(z, true, "native-battery")
    }

    fun seedNative(z: Boolean) {
        if (this.nativeValue == null) {
            setNative(z)
        }
    }

    fun userChanged() {
        this.nativeValue = null
        refresh()
    }

    fun start() {
        this.running = true
        if (!this.registered && this.observeForUser != null) {
            try {
                this.observeForUser.invoke(this.resolver, Settings.System.getUriFor(KEY), false, this.observer, -1)
                this.registered = true
            } catch (e: ReflectiveOperationException) {
            } catch (e: RuntimeException) {
            }
        }
        refresh()
    }

    fun refresh() {
        if (this.nativeValue != null) {
            apply(this.nativeValue!!, true, "native-battery")
            return
        }
        if (this.readForUser != null) {
            try {
                val iIntValue = (this.readForUser.invoke(null, this.resolver, KEY, -1, -2) as Int)
                if (iIntValue == 0 || iIntValue == 1) {
                    apply(iIntValue == 1, true, "system-setting")
                    return
                }
            } catch (e: ReflectiveOperationException) {
            } catch (e: RuntimeException) {
            }
        }
        apply(false, false, "unavailable")
    }

    private fun apply(z: Boolean, z2: Boolean, str: String?) {
        val z3 = this.state.performanceMode xor z
        this.state.performanceMode = z
        this.known = z2
        this.source = str
        if (!z3 || !this.running) {
            return
        }
        this.changed.run()
    }

    fun stop() {
        this.running = false
        if (this.registered) {
            try {
                this.resolver.unregisterContentObserver(this.observer)
                this.registered = false
            } catch (e: RuntimeException) {
            }
        }
    }

    fun diagnostic(): String {
        return "performanceMode=" + this.state.performanceMode + " performanceKnown=" + this.known + " performanceSource=" + this.source + " performanceObserver=" + this.registered
    }

    companion object {
        private const val KEY = "POWER_PERFORMANCE_MODE_OPEN"
    }
}
