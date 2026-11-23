package com.cyzah.circular.domain.settings

import kotlin.math.max

object NoticePolicy {
    @JvmStatic fun stamp(lastUpdateTime: Long, versionCode: Int): String {
        return versionCode.toString() + ":" + max(0L, lastUpdateTime)
    }

    @JvmStatic fun shouldShow(
        freshLaunch: Boolean, dismissed: Boolean,
        savedStamp: String?, currentStamp: String?
    ): Boolean {
        if (!freshLaunch) {
            return false
        }
        if (!dismissed) {
            return true
        }
        return savedStamp == null || savedStamp.isEmpty() || (savedStamp != currentStamp)
    }
}
