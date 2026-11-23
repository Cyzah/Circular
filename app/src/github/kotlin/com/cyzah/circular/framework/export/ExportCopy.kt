package com.cyzah.circular.framework.export

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object ExportCopy {
    private const val BUFFER_SIZE = 65536

    @JvmStatic @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        var count: Int
        while ((input.read(buffer).also { count = it }) != -1) {
            output.write(buffer, 0, count)
            copied += count.toLong()
        }
        output.flush()
        return copied
    }
}
