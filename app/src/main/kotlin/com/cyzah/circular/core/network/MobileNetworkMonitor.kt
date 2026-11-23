package com.cyzah.circular.core.network

import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyCallback.DataConnectionStateListener
import android.telephony.TelephonyCallback.DisplayInfoListener
import android.telephony.TelephonyCallback.ServiceStateListener
import android.telephony.TelephonyCallback.SignalStrengthsListener
import android.telephony.TelephonyDisplayInfo

class MobileNetworkMonitor(private val host: Host) {
    interface Host {
        fun running(): Boolean

        fun isCurrentCell(callback: Cell): Boolean

        fun isCurrentSecondary(callback: Secondary): Boolean

        fun isCurrentRadio(callback: Radio): Boolean

        fun signalLevel(signalStrength: SignalStrength?): Int

        fun primaryRawLevel(level: Int)

        fun applyPrimaryLevel()

        fun applyPrimaryService(serviceState: ServiceState?)

        fun secondaryRawLevel(level: Int)

        fun applySecondaryLevel()

        fun applySecondaryService(serviceState: ServiceState?)

        fun onDisplayInfo(networkType: Int, overrideType: Int)

        fun onDataConnectionState(networkType: Int)

        fun recovered()

        fun changed()
    }

    fun newCell(subscriptionId: Int): Cell {
        return MobileNetworkMonitor.Cell(this, subscriptionId)
    }

    fun newSecondary(subscriptionId: Int): Secondary {
        return MobileNetworkMonitor.Secondary(this, subscriptionId)
    }

    fun newRadio(subscriptionId: Int): Radio {
        return MobileNetworkMonitor.Radio(this, subscriptionId)
    }

    class Cell internal constructor(private val owner: MobileNetworkMonitor, private val id: Int) : TelephonyCallback(),
        SignalStrengthsListener, ServiceStateListener {
        fun subscriptionId(): Int {
            return id
        }

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            if (!owner.host.isCurrentCell(this)) {
                return
            }
            owner.host.primaryRawLevel(owner.host.signalLevel(signalStrength))
            owner.host.applyPrimaryLevel()
            owner.host.recovered()
            owner.host.changed()
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            if (!owner.host.isCurrentCell(this)) {
                return
            }
            owner.host.applyPrimaryService(serviceState)
            owner.host.recovered()
            owner.host.changed()
        }
    }

    class Secondary internal constructor(private val owner: MobileNetworkMonitor, private val id: Int) :
        TelephonyCallback(), SignalStrengthsListener, ServiceStateListener {
        fun subscriptionId(): Int {
            return id
        }

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            if (!owner.host.isCurrentSecondary(this)) {
                return
            }
            owner.host.secondaryRawLevel(owner.host.signalLevel(signalStrength))
            owner.host.applySecondaryLevel()
            owner.host.changed()
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            if (!owner.host.isCurrentSecondary(this)) {
                return
            }
            owner.host.applySecondaryService(serviceState)
            owner.host.changed()
        }
    }

    class Radio internal constructor(private val owner: MobileNetworkMonitor, private val id: Int) : TelephonyCallback(),
        DisplayInfoListener, DataConnectionStateListener {
        fun subscriptionId(): Int {
            return id
        }

        override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
            if (!owner.host.isCurrentRadio(this)) {
                return
            }
            owner.host.onDisplayInfo(
                displayInfo.getNetworkType(),
                displayInfo.getOverrideNetworkType()
            )
            owner.host.changed()
        }

        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
            if (!owner.host.isCurrentRadio(this)) {
                return
            }
            owner.host.onDataConnectionState(networkType)
            owner.host.changed()
        }
    }
}
