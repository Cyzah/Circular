package com.cyzah.circular.hook.runtime

import com.cyzah.circular.hook.runtime.StatusFontAssetsTest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.junit.Assert
import org.junit.Test

class StatusFontAssetsTest {
    @Test
    @Throws(Exception::class)
    fun bundledFontsCoverDigitsAndCustomLatinTechnologyLabelsAtEveryWeight() {
        for (index in STYLES.indices) {
            val resource = "/fonts/SF-Pro-Rounded-" + STYLES[index] + ".otf"
            javaClass.getResourceAsStream(resource).use { input ->
                Assert.assertNotNull("Missing packaged font " + resource, input)
                val bytes = input.readAllBytes()
                Assert.assertTrue("Font was not subset: " + resource, bytes.size < 100000)
                val font = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                Assert.assertEquals("OpenType CFF font", 0x4f54544f, font.getInt(0).toLong())
                val os2: Int = tableOffset(font, "OS/2")
                Assert.assertTrue("Missing weight metadata: " + resource, os2 >= 0)
                Assert.assertEquals(resource, ((index + 1) * 100).toLong(), unsignedShort(font, os2 + 4).toLong())
                Assert.assertTrue(
                    "Kerning layout was dropped: " + resource,
                    tableOffset(font, "GPOS") >= 0
                )
                val cmap: Int = unicodeBmpCmap(font)
                Assert.assertTrue("Missing Unicode cmap: " + resource, cmap >= 0)
                for (codePoint in 0x20..0x7e) {
                    Assert.assertTrue(
                        resource + " missing U+" + Integer.toHexString(codePoint),
                        glyphId(font, cmap, codePoint) != 0
                    )
                }
            }
        }
    }

    companion object {
        private val STYLES = arrayOf<String>(
            "Ultralight", "Thin", "Light", "Regular", "Medium",
            "Semibold", "Bold", "Heavy", "Black"
        )

        private fun tableOffset(font: ByteBuffer, name: String): Int {
            val tag = name.toByteArray(StandardCharsets.US_ASCII)
            val wanted = ByteBuffer.wrap(tag).getInt()
            val tableCount: Int = unsignedShort(font, 4)
            for (index in 0..<tableCount) {
                val offset = 12 + index * 16
                if (font.getInt(offset) == wanted) return font.getInt(offset + 8)
            }
            return -1
        }

        private fun unicodeBmpCmap(font: ByteBuffer): Int {
            val cmap: Int = tableOffset(font, "cmap")
            if (cmap < 0) return -1
            val count: Int = unsignedShort(font, cmap + 2)
            for (index in 0..<count) {
                val record = cmap + 4 + index * 8
                val platform: Int = unsignedShort(font, record)
                val encoding: Int = unsignedShort(font, record + 2)
                val subtable = cmap + font.getInt(record + 4)
                if ((platform == 0 || (platform == 3 && encoding == 1))
                    && unsignedShort(font, subtable) == 4
                ) return subtable
            }
            return -1
        }

        private fun glyphId(font: ByteBuffer, cmap: Int, codePoint: Int): Int {
            val segments: Int = unsignedShort(font, cmap + 6) / 2
            val ends = cmap + 14
            val starts = ends + 2 * segments + 2
            val deltas = starts + 2 * segments
            val ranges = deltas + 2 * segments
            for (index in 0..<segments) {
                if (codePoint > unsignedShort(font, ends + 2 * index)) continue
                val start: Int = unsignedShort(font, starts + 2 * index)
                if (codePoint < start) return 0
                val delta = font.getShort(deltas + 2 * index).toInt()
                val rangeOffset = ranges + 2 * index
                val range: Int = unsignedShort(font, rangeOffset)
                if (range == 0) return (codePoint + delta) and 0xffff
                val glyph: Int = unsignedShort(font, rangeOffset + range + 2 * (codePoint - start))
                return if (glyph == 0) 0 else (glyph + delta) and 0xffff
            }
            return 0
        }

        private fun unsignedShort(font: ByteBuffer, offset: Int): Int {
            return font.getShort(offset).toInt() and 0xffff
        }
    }
}
