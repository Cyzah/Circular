package com.cyzah.circular.domain.settings

/** One owned subscription; a null key means the source cannot identify the change.  */
interface ObservableSettingsValues : SettingsValues {
    interface Listener {
        fun onChanged(key: String?)
    }

    fun register(listener: Listener)
    fun unregister()
}
