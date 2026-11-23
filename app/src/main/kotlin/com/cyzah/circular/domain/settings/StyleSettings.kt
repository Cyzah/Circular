package com.cyzah.circular.domain.settings

/** Owns active appearance, per-style archives and atomic style transitions.  */
class StyleSettings(private val store: Store) {
    interface Store : SettingsValues {
        fun edit(): Editor
    }

    interface Editor {
        fun putInt(key: String?, value: Int): Editor
        fun putBoolean(key: String?, value: Boolean): Editor
        fun putString(key: String?, value: String?): Editor
        fun apply()
    }

    fun selected(): DualStyle {
        return DualStyle.Companion.from(
            store.getBoolean(SettingsSchema.GROUPED_DOTS, false),
            store.getBoolean(SettingsSchema.CAPSULE_DUAL, false),
            store.getBoolean(SettingsSchema.DUO_DUAL, false)
        )
    }

    fun initialize(dualSimAvailable: Boolean): Boolean {
        var changed = ensureAvailable(dualSimAvailable)
        val style = selected()
        val configuredSize = DisplayPolicy.size(store.getInt(SettingsSchema.SIZE, 24))
        val size = SizePolicy.clamp(style, configuredSize)
        val swap = store.getBoolean(SettingsSchema.SWAP_CONTENTS, false)
        if (configuredSize != size || (style == DualStyle.DUO && swap)) {
            store.edit()!!.putInt(SettingsSchema.SIZE, size)!!
                .putBoolean(
                    com.cyzah.circular.domain.settings.SettingsSchema.SWAP_CONTENTS,
                    style != com.cyzah.circular.domain.settings.DualStyle.DUO && swap
                )!!
                .apply()
            changed = true
        }
        return changed
    }

    fun ensureAvailable(dualSimAvailable: Boolean): Boolean {
        return select(
            DualStyleAvailability.fallback(selected(), dualSimAvailable)!!,
            dualSimAvailable
        )
    }

    fun select(target: DualStyle, dualSimAvailable: Boolean): Boolean {
        val previous = selected()
        if (target == previous || !DualStyleAvailability.allows(target, dualSimAvailable)) {
            return false
        }
        val source = currentProfile()
        var profile: StyleProfile? = StyleProfile.Companion.decode(store.getString(profileKey(target), ""))
        if (profile == null) {
            profile = StyleProfile.Companion.defaults(target)
        }
        if (profile == null) {
            profile = source
        }
        profile = normalized(target, profile)
        val editor = store.edit()!!
            .putString(profileKey(previous), source.encode())
            .putBoolean(
                com.cyzah.circular.domain.settings.SettingsSchema.GROUPED_DOTS,
                target == com.cyzah.circular.domain.settings.DualStyle.GROUPED
            )!!
            .putBoolean(
                com.cyzah.circular.domain.settings.SettingsSchema.CAPSULE_DUAL,
                target == com.cyzah.circular.domain.settings.DualStyle.CAPSULE
            )!!
            .putBoolean(
                com.cyzah.circular.domain.settings.SettingsSchema.DUO_DUAL,
                target == com.cyzah.circular.domain.settings.DualStyle.DUO
            )!!
            .putString(profileKey(target), profile.encode())
        writeProfile(editor, profile)
        editor.apply()
        return true
    }

    fun currentProfile(): StyleProfile {
        // These are the provider's migrated preference rules. Snapshot decoding also
        // accepts older wire formats and intentionally has different fallback rules.
        return StyleProfile(
            DisplayPolicy.size(store.getInt(SettingsSchema.SIZE, 24)),
            SettingsRowSerializer.ringWeight(store),
            DisplayPolicy.centrePercent(store.getInt(SettingsSchema.WIFI_SCALE, 100)),
            SettingsRowSerializer.networkWeight(store, SettingsSchema.WIFI_WEIGHT),
            SettingsRowSerializer.technologyScale(store),
            SettingsRowSerializer.technologyWeight(store),
            store.getBoolean(SettingsSchema.SHOW_PERCENT, false),
            SettingsRowSerializer.percentScale(store),
            SettingsRowSerializer.percentWeight(store),
            store.getBoolean(SettingsSchema.SWAP_CONTENTS, false),
            SettingsRowSerializer.contentOffsets(store)
        )
    }

    companion object {
        @JvmStatic fun profileKey(style: DualStyle): String {
            return "style_profile_" + style.name.lowercase()
        }

        private fun normalized(style: DualStyle?, profile: StyleProfile): StyleProfile {
            val size = SizePolicy.clamp(style, profile.size)
            val swap = style != DualStyle.DUO && profile.swapContents
            if (size == profile.size && swap == profile.swapContents) {
                return profile
            }
            return StyleProfile(
                size, profile.weight, profile.wifiScale, profile.wifiWeight,
                profile.technologyScale, profile.technologyWeight, profile.showPercent,
                profile.percentScale, profile.percentWeight, swap, profile.contentOffsets
            )
        }

        private fun writeProfile(editor: Editor, profile: StyleProfile) {
            editor.putInt(SettingsSchema.SIZE, profile.size)!!
                .putInt(com.cyzah.circular.domain.settings.SettingsSchema.WEIGHT_V2, profile.weight)!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.WEIGHT,
                    com.cyzah.circular.domain.settings.AppearanceScale.weightToLegacy(profile.weight)
                )!!
                .putInt(com.cyzah.circular.domain.settings.SettingsSchema.WIFI_SCALE, profile.wifiScale)!!
                .putInt(com.cyzah.circular.domain.settings.SettingsSchema.WIFI_WEIGHT, profile.wifiWeight)!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TECHNOLOGY_SCALE_V2,
                    profile.technologyScale
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TECHNOLOGY_SCALE,
                    com.cyzah.circular.domain.settings.AppearanceScale.sizeToLegacy(profile.technologyScale)
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TECHNOLOGY_WEIGHT_V2,
                    profile.technologyWeight
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TECHNOLOGY_WEIGHT,
                    com.cyzah.circular.domain.settings.AppearanceScale.weightToLegacy(profile.technologyWeight)
                )!!
                .putBoolean(com.cyzah.circular.domain.settings.SettingsSchema.SHOW_PERCENT, profile.showPercent)!!
                .putInt(com.cyzah.circular.domain.settings.SettingsSchema.PERCENT_SCALE_V2, profile.percentScale)!!
                .putInt(com.cyzah.circular.domain.settings.SettingsSchema.PERCENT_WEIGHT_V2, profile.percentWeight)!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.PERCENT_WEIGHT,
                    com.cyzah.circular.core.graphics.BatteryHeader.weightToLegacy(profile.percentWeight)
                )!!
                .putBoolean(com.cyzah.circular.domain.settings.SettingsSchema.SWAP_CONTENTS, profile.swapContents)!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TOP_CONTENT_OFFSET_X_PX,
                    profile.contentOffsets.topX / 2
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TOP_CONTENT_OFFSET_Y_PX,
                    profile.contentOffsets.topY / 2
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.CENTER_CONTENT_OFFSET_X_PX,
                    profile.contentOffsets.centerX / 2
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.CENTER_CONTENT_OFFSET_Y_PX,
                    profile.contentOffsets.centerY / 2
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.BOTTOM_CONTENT_OFFSET_X_PX,
                    profile.contentOffsets.bottomX / 2
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_PX,
                    profile.contentOffsets.bottomY / 2
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TOP_CONTENT_OFFSET_X_HALF_PX,
                    profile.contentOffsets.topX
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.TOP_CONTENT_OFFSET_Y_HALF_PX,
                    profile.contentOffsets.topY
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.CENTER_CONTENT_OFFSET_X_HALF_PX,
                    profile.contentOffsets.centerX
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.CENTER_CONTENT_OFFSET_Y_HALF_PX,
                    profile.contentOffsets.centerY
                )!!
                .putInt(
                    com.cyzah.circular.domain.settings.SettingsSchema.BOTTOM_CONTENT_OFFSET_X_HALF_PX,
                    profile.contentOffsets.bottomX
                )!!
                .putInt(SettingsSchema.BOTTOM_CONTENT_OFFSET_Y_HALF_PX, profile.contentOffsets.bottomY)
        }
    }
}
