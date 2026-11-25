package com.cyzah.circular.data.settings

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.cyzah.circular.domain.settings.SettingsSchema
import com.cyzah.circular.domain.settings.StatusFontSelection
import java.io.DataInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

/** Stores one imported status font in the module app's device-protected storage. */
object StatusFontFiles {
    const val NAME_PREFERENCE = "status_font_name"
    private const val TOKEN_PREFERENCE = "status_font_token"
    private const val CUSTOM_PREFIX = "custom:"
    private const val MAX_FILE_BYTES = 64L * 1024 * 1024

    internal fun maxImportBytes(): Long = MAX_FILE_BYTES

    @Synchronized
    @Throws(IOException::class)
    fun importFont(context: Context, uri: Uri) {
        val directory = directory(context)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Cannot create status font directory")
        }
        val token = UUID.randomUUID().toString().replace("-", "")
        val destination = fontFile(directory, token)
        val preferences = ConfigProvider.prefs(context)
        val previousSource = preferences.getString(SettingsSchema.STATUS_FONT_SOURCE, null)
        val previousName = preferences.getString(NAME_PREFERENCE, null)
        val previousToken = preferences.getString(TOKEN_PREFERENCE, null)
        if (!destination.createNewFile()) throw IOException("Cannot create status font file")

        var saved = false
        try {
            copyFont(context, uri, destination)
            val signature = DataInputStream(destination.inputStream().buffered()).use { it.readInt() }
            if (signature != 0x00010000 && signature != 0x4F54544F && signature != 0x74727565) {
                throw IOException("Expected a TTF or OTF font")
            }
            typeface(Font.Builder(destination).build())
            saved = preferences.edit()
                .putString(SettingsSchema.STATUS_FONT_SOURCE, CUSTOM_PREFIX + token)
                .putString(NAME_PREFERENCE, displayName(context, uri))
                .putString(TOKEN_PREFERENCE, token)
                .commit()
            if (!saved) {
                preferences.edit()
                    .putString(SettingsSchema.STATUS_FONT_SOURCE, previousSource)
                    .putString(NAME_PREFERENCE, previousName)
                    .putString(TOKEN_PREFERENCE, previousToken)
                    .commit()
                throw IOException("Cannot save status font selection")
            }
        } finally {
            if (!saved) destination.delete()
        }
        if (previousToken != null && StatusFontSelection.isValidToken(previousToken)) {
            fontFile(directory, previousToken).delete()
        }
    }

    fun savedSelection(context: Context): StatusFontSelection? {
        val token = ConfigProvider.prefs(context).getString(TOKEN_PREFERENCE, null) ?: return null
        if (!StatusFontSelection.isValidToken(token) || !fontFile(directory(context), token).isFile) {
            return null
        }
        return StatusFontSelection.decode(CUSTOM_PREFIX + token)
    }

    fun load(context: Context, token: String): Typeface? {
        if (!StatusFontSelection.isValidToken(token)) return null
        return try {
            context.contentResolver.openFileDescriptor(fontUri(token), "r")?.use {
                typeface(Font.Builder(it).build())
            }
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    @Throws(FileNotFoundException::class)
    internal fun openSelectedFont(context: Context, uri: Uri): ParcelFileDescriptor {
        val token = uri.lastPathSegment
        if (token == null || !StatusFontSelection.isValidToken(token) || uri != fontUri(token)) {
            throw FileNotFoundException("Unknown status font URI")
        }
        val selected = ConfigProvider.prefs(context).getString(SettingsSchema.STATUS_FONT_SOURCE, null)
        if (selected != CUSTOM_PREFIX + token) throw FileNotFoundException("Status font unavailable")
        return ParcelFileDescriptor.open(fontFile(directory(context), token), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun copyFont(context: Context, uri: Uri, destination: File) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read status font")
        input.use { source ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_FILE_BYTES) throw IOException("Status font exceeds 32 MiB")
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
    }

    private fun displayName(context: Context, uri: Uri): String {
        val name = try {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME)
            context.contentResolver.query(uri, columns, null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: RuntimeException) {
            null
        }
        return name.orEmpty().take(120).filterNot(Char::isISOControl).trim()
    }

    private fun typeface(font: Font): Typeface =
        Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
            .setSystemFallback("sans-serif")
            .setStyle(font.style)
            .build()

    private fun directory(context: Context): File =
        File(context.createDeviceProtectedStorageContext().filesDir, "status-fonts")

    private fun fontFile(directory: File, token: String): File = File(directory, "$token.font")

    private fun fontUri(token: String): Uri =
        ConfigProvider.URI.buildUpon().appendPath("font").appendPath(token).build()
}
