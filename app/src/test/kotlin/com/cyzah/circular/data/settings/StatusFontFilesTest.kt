package com.cyzah.circular.data.settings

import org.junit.Assert.assertTrue
import org.junit.Test

class StatusFontFilesTest {
    @Test
    fun importLimitAcceptsTheProvidedVariableFontSize() {
        assertTrue(StatusFontFiles.maxImportBytes() >= 35_237_292L)
    }
}
