package com.cyzah.circular.domain.settings


class StyleProfile @JvmOverloads constructor(
    @JvmField val size: Int, @JvmField val weight: Int, @JvmField val wifiScale: Int, @JvmField val wifiWeight: Int, @JvmField val technologyScale: Int,
    @JvmField val technologyWeight: Int, @JvmField val showPercent: Boolean, @JvmField val percentScale: Int, @JvmField val percentWeight: Int,
    @JvmField val swapContents: Boolean, contentOffsets: ContentOffsets? = ContentOffsets.DEFAULT
) {
    @JvmField val contentOffsets: ContentOffsets

    init {
        this.contentOffsets = if (contentOffsets == null) ContentOffsets.DEFAULT else contentOffsets
    }

    fun encode(): String {
        return (VERSION + "|" + size + "|" + weight + "|" + wifiScale + "|" + wifiWeight
                + "|" + technologyScale + "|" + technologyWeight + "|" + (if (showPercent) 1 else 0)
                + "|" + percentScale + "|" + percentWeight + "|" + (if (swapContents) 1 else 0)
                + "|" + contentOffsets.topX + "|" + contentOffsets.topY
                + "|" + contentOffsets.centerX + "|" + contentOffsets.centerY
                + "|" + contentOffsets.bottomX + "|" + contentOffsets.bottomY)
    }

    fun sameAs(other: StyleProfile?): Boolean {
        return other != null && size == other.size && weight == other.weight && wifiScale == other.wifiScale && wifiWeight == other.wifiWeight && technologyScale == other.technologyScale && technologyWeight == other.technologyWeight && showPercent == other.showPercent && percentScale == other.percentScale && percentWeight == other.percentWeight && swapContents == other.swapContents && contentOffsets.equals(
            other.contentOffsets
        )
    }

    companion object {
        private const val VERSION = "v3"
        @JvmStatic fun defaults(style: DualStyle?): StyleProfile? {
            if (style == DualStyle.CAPSULE) {
                return StyleProfile(22, 135, 120, 110, 110, 110, false, 80, 145, false)
            }
            if (style == DualStyle.DUO) {
                return StyleProfile(26, 100, 95, 110, 95, 110, true, 100, 140, false)
            }
            return null
        }

        @JvmStatic fun decode(value: String?): StyleProfile? {
            if (value == null) {
                return null
            }
            val parts: Array<String?> = value.split("\\|".toRegex()).toTypedArray()
            val legacy = parts.size == 11 && "v1" == parts[0]
            val legacyPixels = parts.size == 17 && "v2" == parts[0]
            if (!legacy && !legacyPixels && !(parts.size == 17 && VERSION == parts[0])) {
                return null
            }
            try {
                var offsets = ContentOffsets.DEFAULT
                if (!legacy) {
                    val topX = parts[11]!!.toInt()
                    val topY = parts[12]!!.toInt()
                    val centerX = parts[13]!!.toInt()
                    val centerY = parts[14]!!.toInt()
                    val bottomX = parts[15]!!.toInt()
                    val bottomY = parts[16]!!.toInt()
                    offsets = if (legacyPixels)
                        ContentOffsets.fromLegacyPixels(topX, topY, centerX, centerY, bottomX, bottomY)
                    else
                        ContentOffsets(topX, topY, centerX, centerY, bottomX, bottomY)
                }
                return StyleProfile(
                    parts[1]!!.toInt(),
                    parts[2]!!.toInt(),
                    parts[3]!!.toInt(),
                    parts[4]!!.toInt(),
                    parts[5]!!.toInt(),
                    parts[6]!!.toInt(),
                    parseBoolean(parts[7]),
                    parts[8]!!.toInt(),
                    parts[9]!!.toInt(),
                    parseBoolean(parts[10]), offsets
                )
            } catch (exception: NumberFormatException) {
                return null
            }
        }

        private fun parseBoolean(value: String?): Boolean {
            if ("1" == value) {
                return true
            }
            if ("0" == value) {
                return false
            }
            throw NumberFormatException("boolean")
        }
    }
}
