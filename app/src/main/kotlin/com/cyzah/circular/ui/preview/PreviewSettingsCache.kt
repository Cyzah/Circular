package com.cyzah.circular.ui.preview

import com.cyzah.circular.domain.settings.AppearanceSettings
import com.cyzah.circular.domain.settings.ObservableSettingsValues
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.domain.settings.SettingsSnapshot

/** A view's immutable settings pair, rebuilt once after preference changes.  */
class PreviewSettingsCache(private val source: ObservableSettingsValues, private val invalidate: Runnable) {
    private val changed: ObservableSettingsValues.Listener = object : ObservableSettingsValues.Listener {
        override fun onChanged(key: String?) {
            if (SettingsSchema.affectsAppearance(key)) {
                dirty = true
                invalidate.run()
            }
        }
    }
    private var attached = false
    private var dirty = true
    private var settings: AppearanceSettings? = null
    private var expanded: AppearanceSettings? = null
    private var technologyText: String? = null

    fun attach() {
        if (attached) {
            return
        }
        source.register(changed)
        attached = true
        // Changes while detached have no listener, so reattachment always refreshes.
        changed.onChanged(null)
    }

    fun detach() {
        if (attached) {
            source.unregister()
            attached = false
        }
    }

    fun settings(): AppearanceSettings {
        refresh()
        return settings!!
    }

    fun expanded(): AppearanceSettings {
        refresh()
        return expanded!!
    }

    fun technologyText(): String {
        refresh()
        return technologyText!!
    }

    private fun refresh() {
        if (dirty) {
            val snapshot: SettingsSnapshot = SettingsSnapshot.Companion.from(source)
            settings = AppearanceSettings.Companion.from(snapshot)
            expanded = settings!!.controlCenterAppearance()
            technologyText = snapshot.technologyText()
            dirty = false
        }
    }
}
