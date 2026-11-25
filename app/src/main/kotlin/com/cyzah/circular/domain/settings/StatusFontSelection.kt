package com.cyzah.circular.domain.settings

enum class StatusFontMode { DEFAULT, SYSTEM, CUSTOM }

@ConsistentCopyVisibility
data class StatusFontSelection private constructor(
    val mode: StatusFontMode,
    val customToken: String? = null
) {
    fun encode(): String = when (mode) {
        StatusFontMode.DEFAULT -> "default"
        StatusFontMode.SYSTEM -> "system"
        StatusFontMode.CUSTOM -> "custom:$customToken"
    }

    companion object {
        val DEFAULT = StatusFontSelection(StatusFontMode.DEFAULT)
        val SYSTEM = StatusFontSelection(StatusFontMode.SYSTEM)
        private const val CUSTOM_PREFIX = "custom:"

        fun decode(value: String?): StatusFontSelection {
            if (value == "system") return SYSTEM
            if (value == null || !value.startsWith(CUSTOM_PREFIX)) return DEFAULT
            val token = value.removePrefix(CUSTOM_PREFIX)
            return if (isValidToken(token)) StatusFontSelection(StatusFontMode.CUSTOM, token) else DEFAULT
        }

        fun isValidToken(token: String): Boolean =
            token.length == 32 && token.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
