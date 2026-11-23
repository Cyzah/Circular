package com.cyzah.circular.domain.settings

/** Coalesces one main-thread source subscription without dropping independent report events.  */
class SettingsObservation(
    private val source: ObservableSettingsValues,
    private val scheduler: Scheduler,
    private val listener: Listener
) {
    interface Scheduler {
        fun post(task: Runnable)
        fun remove(task: Runnable)
    }

    interface Listener {
        fun onAppearanceChanged(snapshot: SettingsSnapshot)
        fun onSceneChanged()
        fun onReportChanged()
    }

    private var snapshot: SettingsSnapshot? = null
    private var attached = false
    private var queued = false
    private var appearanceDirty = false
    private var sceneDirty = false
    private var reportDirty = false
    private val work: Runnable = object : Runnable {
        override fun run() {
            queued = false
            if (!attached) return
            if (appearanceDirty) refreshAppearanceNow()
            val scene = sceneDirty
            val report = reportDirty
            sceneDirty = false
            reportDirty = false
            if (scene) listener.onSceneChanged()
            if (report) listener.onReportChanged()
        }
    }
    private val changed: ObservableSettingsValues.Listener = object : ObservableSettingsValues.Listener {
        override fun onChanged(key: String?) {
            appearanceDirty = appearanceDirty or SettingsSchema.affectsAppearance(key)
            sceneDirty = sceneDirty or (key == null || SettingsSchema.isSceneKey(key))
            reportDirty = reportDirty or (key == null || SettingsSchema.isReportKey(key))
            if (!queued) {
                queued = true
                scheduler.post(work)
            }
        }
    }

    fun attach() {
        if (attached) return
        attached = true
        source.register(changed)
        snapshot = SettingsSnapshot.Companion.from(source)
        appearanceDirty = false
    }

    fun detach() {
        if (!attached) return
        attached = false
        source.unregister()
        scheduler.remove(work)
        reportDirty = false
        sceneDirty = reportDirty
        appearanceDirty = sceneDirty
        queued = appearanceDirty
    }

    fun snapshot(): SettingsSnapshot? {
        return snapshot
    }

    fun refreshAppearanceNow() {
        if (!attached) return
        appearanceDirty = false
        val latest: SettingsSnapshot = SettingsSnapshot.Companion.from(source)
        val different = snapshot == null || !snapshot!!.sameAppearanceAs(latest)
        snapshot = latest
        if (different) listener.onAppearanceChanged(latest)
    }
}
