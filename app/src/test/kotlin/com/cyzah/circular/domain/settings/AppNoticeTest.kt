package com.cyzah.circular.domain.settings

import org.junit.Assert
import org.junit.Test

class AppNoticeTest {
    @Test
    fun noticeWelcomesUserAndClarifiesMaintenanceBoundaries() {
        Assert.assertEquals("使用须知", AppNotice.TITLE)
        Assert.assertTrue(AppNotice.MESSAGE.contains("欢迎来到 Circular"))
        Assert.assertTrue(AppNotice.MESSAGE.contains("不做其他机型的适配"))
        Assert.assertTrue(AppNotice.MESSAGE.contains("不受理适配请求"))
        Assert.assertTrue(AppNotice.MESSAGE.contains("无法使用、功能不生效等问题，不在处理范围内"))
        Assert.assertTrue(AppNotice.MESSAGE.contains("动画本身的 Bug，我会继续修复"))
    }

    @Test
    fun noticeUsesCurrentProductLanguageWithoutLegacyClaims() {
        val text = AppNotice.TITLE + AppNotice.MESSAGE + AppNotice.DESCRIPTION
        for (legacy in arrayOf<String>("二改", "二次修改", "AI", "最新OS4")) {
            Assert.assertFalse(legacy, text.contains(legacy))
        }
        Assert.assertFalse(AppNotice.MESSAGE.contains("反馈"))
        Assert.assertTrue(AppNotice.MESSAGE.contains("机型或系统差异"))
    }
}
