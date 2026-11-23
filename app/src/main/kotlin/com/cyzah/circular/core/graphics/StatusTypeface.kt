package com.cyzah.circular.core.graphics

import android.graphics.Typeface
import android.graphics.Typeface.CustomFallbackBuilder
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/** Shared rounded status font, loaded only when a renderer first asks for it.  */
object StatusTypeface {
    private val STYLES = arrayOf<String?>(
        "Ultralight", "Thin", "Light", "Regular", "Medium",
        "Semibold", "Bold", "Heavy", "Black"
    )
    private val CACHE = arrayOfNulls<Typeface>(1001)
    private var family: FontFamily? = null
    private var initialized = false

    @JvmStatic @Synchronized
    fun forWeight(openTypeWeight: Int): Typeface? {
        val weight = max(1, min(1000, openTypeWeight))
        if (CACHE[weight] != null) return CACHE[weight]
        initialize()
        if (CACHE[weight] == null) {
            CACHE[weight] = if (family == null)
                Typeface.create(Typeface.SANS_SERIF, weight, false)
            else
                create(weight)
        }
        return CACHE[weight]
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        try {
            var builder: FontFamily.Builder? = null
            for (index in STYLES.indices) {
                val font: Font? = Font.Builder(StatusTypeface.readFont(STYLES[index]!!))
                    .setWeight((index + 1) * 100)
                    .setSlant(FontStyle.FONT_SLANT_UPRIGHT)
                    .build()
                if (builder == null) builder = FontFamily.Builder(font!!)
                else builder.addFont(font!!)
            }
            family = builder!!.build()
            CACHE[600] = create(600)
            Log.i("Circular", "SF Pro Rounded status font loaded: 9 weights, default=600")
        } catch (failure: Exception) {
            family = null
            Log.w("Circular", "Bundled status font unavailable; using sans-serif", failure)
        } catch (failure: LinkageError) {
            family = null
            Log.w("Circular", "Bundled status font unavailable; using sans-serif", failure)
        }
    }

    private fun create(weight: Int): Typeface? {
        return CustomFallbackBuilder(family!!)
            .setSystemFallback("sans-serif")
            .setStyle(FontStyle(weight, FontStyle.FONT_SLANT_UPRIGHT))
            .build()
    }

    @Throws(IOException::class)
    private fun readFont(style: String): ByteBuffer {
        val resource = "/fonts/SF-Pro-Rounded-" + style + ".otf"
        StatusTypeface::class.java.getResourceAsStream(resource).use { input ->
            if (input == null) throw IOException("Missing " + resource)
            val output = ByteArrayOutputStream(32 * 1024)
            val chunk = ByteArray(8192)
            var count: Int
            while ((input.read(chunk).also { count = it }) != -1) output.write(chunk, 0, count)
            val bytes = output.toByteArray()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.put(bytes)
            buffer.flip()
            return buffer
        }
    }
}
