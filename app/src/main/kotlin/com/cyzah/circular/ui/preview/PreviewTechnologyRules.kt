package com.cyzah.circular.ui.preview

import com.cyzah.circular.core.graphics.TechnologyText
import com.cyzah.circular.core.network.IconState

/** Keeps rule substitutions in the drawing input, never in the saved scene.  */
class PreviewTechnologyRules {
    private val renderState = IconState()
    private var sourceState: IconState? = null
    private var sceneVersion = 0
    private var ruleText: String? = null
    private var parsed: TechnologyText? = null

    fun update(rawState: IconState, version: Int, specification: String?): IconState {
        val nextRules = if (specification == null) "" else specification
        val rulesChanged = nextRules != ruleText
        if (rulesChanged) {
            parsed = TechnologyText.Companion.parse(nextRules)
            ruleText = nextRules
        }
        if (rulesChanged || sourceState != rawState || sceneVersion != version) {
            renderState.copyFrom(rawState)
            renderState.mobileTechnology = checkNotNull(parsed!!.apply(rawState.mobileTechnology))
            sourceState = rawState
            sceneVersion = version
        }
        return renderState
    }
}
