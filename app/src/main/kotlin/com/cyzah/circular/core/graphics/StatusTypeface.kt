package com.cyzah.circular.core.graphics

import android.graphics.Typeface
import android.graphics.Typeface.CustomFallbackBuilder
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.util.Log
import com.cyzah.circular.domain.settings.StatusFontMode
import com.cyzah.circular.domain.settings.StatusFontSelection
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

/** Resolved font and its weighted faces, shared by the painters in one process. */
internal class StatusFont private constructor(
    val selection: StatusFontSelection,
    private val base: Typeface? = null
) {
    private val weighted = mutableMapOf<Int, Typeface>()

    fun forWeight(weight: Int): Typeface? {
        if (selection.mode == StatusFontMode.DEFAULT) return StatusTypeface.forWeight(weight)
        return weighted.getOrPut(weight) { Typeface.create(base ?: Typeface.DEFAULT, weight, false) }
    }

    companion object {
        val DEFAULT = StatusFont(StatusFontSelection.DEFAULT)
        val SYSTEM = StatusFont(StatusFontSelection.SYSTEM)

        fun resolve(selection: StatusFontSelection, load: (String) -> Typeface?): StatusFont {
            return when (selection.mode) {
                StatusFontMode.DEFAULT -> DEFAULT
                StatusFontMode.SYSTEM -> SYSTEM
                StatusFontMode.CUSTOM -> {
                    val face = selection.customToken?.let(load)
                    if (face == null) DEFAULT else StatusFont(selection, face)
                }
            }
        }
    }
}
