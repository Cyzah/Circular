package com.cyzah.circular.framework.export

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.cyzah.circular.BuildConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.ArrayList

class ExportFileProvider : ContentProvider() {
    // android.content.ContentProvider
    override fun attachInfo(context: Context?, providerInfo: ProviderInfo) {
        if (providerInfo.exported || !providerInfo.grantUriPermissions) {
            throw SecurityException("Export provider must be private with URI grants")
        }
        super.attachInfo(context, providerInfo)
    }

    @Throws(FileNotFoundException::class)
    private fun resolve(uri: Uri): File {
        if (("content" != uri.getScheme()) || (AUTHORITY != uri.getAuthority()) || uri.getPathSegments().size != 1 || uri.getQuery() != null || uri.getFragment() != null) {
            throw FileNotFoundException("Invalid export URI")
        }
        try {
            val providerContext = context ?: throw FileNotFoundException("Export provider is unavailable")
            val name = uri.lastPathSegment ?: throw FileNotFoundException("Invalid export URI")
            return ExportPolicy.sharedFile(SystemUiExportController.directory(providerContext), name)
        } catch (e: IOException) {
            throw FileNotFoundException(e.message)
        }
    }

    // android.content.ContentProvider
    override fun onCreate(): Boolean {
        return true
    }

    // android.content.ContentProvider
    override fun getType(uri: Uri): String {
        try {
            resolve(uri)
            return "application/zip"
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException(e)
        }
    }

    @Throws(FileNotFoundException::class)  // android.content.ContentProvider
    override fun openFile(uri: Uri, str: String): ParcelFileDescriptor? {
        if ("r" != str) {
            throw SecurityException("Read-only export")
        }
        return ParcelFileDescriptor.open(resolve(uri), 268435456)
    }

    // android.content.ContentProvider
    override fun query(
        uri: Uri,
        strArr: Array<String?>?,
        str: String?,
        strArr2: Array<String?>?,
        str2: String?
    ): Cursor {
        try {
            val fileResolve = resolve(uri)
            val requestedColumns = strArr ?: arrayOf("_display_name", "_size")
            val columns = ArrayList<String>()
            val values = ArrayList<Any>()
            for (str3 in requestedColumns) {
                if ("_display_name" == str3) {
                    columns.add("_display_name")
                    values.add(fileResolve.name)
                } else if ("_size" == str3) {
                    columns.add("_size")
                    values.add(fileResolve.length())
                }
            }
            val matrixCursor = MatrixCursor(columns.toTypedArray(), 1)
            matrixCursor.addRow(values)
            return matrixCursor
        } catch (e: FileNotFoundException) {
            throw IllegalArgumentException(e)
        }
    }

    // android.content.ContentProvider
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        throw UnsupportedOperationException("Read-only export")
    }

    // android.content.ContentProvider
    override fun update(uri: Uri, contentValues: ContentValues?, str: String?, strArr: Array<String?>?): Int {
        throw UnsupportedOperationException("Read-only export")
    }

    // android.content.ContentProvider
    override fun delete(uri: Uri, str: String?, strArr: Array<String?>?): Int {
        throw UnsupportedOperationException("Read-only export")
    }

    companion object {
        const val AUTHORITY: String = BuildConfig.APPLICATION_ID + ".exports"

        @JvmStatic @Throws(IOException::class)
        fun uri(context: Context, file: File): Uri {
            if (ExportPolicy.sharedFile(
                    SystemUiExportController.directory(context),
                    file.getName()
                ) != file.getCanonicalFile()
            ) {
                throw IOException("文件不在导出目录。")
            }
            return Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(file.getName()).build()
        }
    }
}
