package com.cyzah.circular.domain.settings

import android.graphics.Typeface
import com.cyzah.circular.core.graphics.StatusFontWeight
import com.cyzah.circular.core.graphics.StatusTypeface
import com.cyzah.circular.core.graphics.geometry.SignalSpacing
import java.util.LinkedHashMap
import kotlin.math.max
import kotlin.math.min

object AppearanceScale {
    @JvmStatic fun ringWeight(i: Int): Float {
        return SignalSpacing.scaleFromPercent(i)
    }

    @JvmStatic fun ringTop(i: Int): Float {
        return max(8.0f, (ringWeight(i) * 3.75f) + 0.5f)
    }

    @JvmStatic fun technologyTextSize(i: Int): Float {
        return (DisplayPolicy.centrePercent(i) * 36) / 100.0f
    }

    @JvmStatic fun technologyTextWeight(i: Int): Int {
        return StatusFontWeight.technology(i)
    }

    /** Bundled native weights retain a distinct light and heavy end of the slider.  */
    @JvmStatic fun technologyTypeface(i: Int): Typeface? {
        return StatusTypeface.forWeight(StatusFontWeight.technology(i))
    }

    /**
     * 制式文字在最粗端额外叠的描边比例（相对字号）：110% 以上真的更粗，100%（默认）及以下为 0。
     * 180% 时约 5%，大致相当于再重半档。
     */
    @JvmStatic fun technologyWeightStrokeRatio(i: Int): Float {
        val iPercent = DisplayPolicy.weight(i)
        if (iPercent <= 105) {
            return 0.0f
        }
        return min(0.12f, (iPercent - 105) * 0.0016f)
    }

    @JvmStatic fun weightFromLegacy(i: Int): Int {
        return DisplayPolicy.weight(Math.round(DisplayPolicy.weight(i) / 6.0f) * 5)
    }

    @JvmStatic fun sizeFromLegacy(i: Int): Int {
        return DisplayPolicy.centrePercent(Math.round(DisplayPolicy.centrePercent(i) / 6.0f) * 5)
    }

    @JvmStatic fun weightToLegacy(i: Int): Int {
        return DisplayPolicy.weight(Math.round(DisplayPolicy.weight(i) * 1.2f))
    }

    @JvmStatic fun sizeToLegacy(i: Int): Int {
        return DisplayPolicy.centrePercent(Math.round(DisplayPolicy.centrePercent(i) * 1.2f))
    }

    private fun integer(map: MutableMap<String?, *>, str: String?, i: Int): Int {
        val obj: Any? = map.get(str)
        return if (obj is Int) obj else i
    }

    @JvmStatic fun migration(map: MutableMap<String?, *>): MutableMap<String?, Int?> {
        val linkedHashMap = LinkedHashMap<String?, Int?>()
        var iWeightFromLegacy = 100
        val iWeight = DisplayPolicy.weight(integer(map, "weight", 100))
        if (!map.containsKey("wifi_weight")) {
            linkedHashMap.put("wifi_weight", iWeight)
        }
        if (!map.containsKey("weight_v2")) {
            linkedHashMap.put("weight_v2", if (map.containsKey("weight")) weightFromLegacy(iWeight) else 100)
        }
        if (!map.containsKey("technology_scale_v2")) {
            linkedHashMap.put(
                "technology_scale_v2",
                if (map.containsKey("technology_scale")) sizeFromLegacy(integer(map, "technology_scale", 100)) else 100
            )
        }
        if (!map.containsKey("technology_weight_v2")) {
            if (map.containsKey("technology_weight")) {
                iWeightFromLegacy = weightFromLegacy(integer(map, "technology_weight", 100))
            } else if (map.containsKey("weight")) {
                iWeightFromLegacy = weightFromLegacy(iWeight)
            }
            linkedHashMap.put("technology_weight_v2", iWeightFromLegacy)
        }
        return linkedHashMap
    }
}
