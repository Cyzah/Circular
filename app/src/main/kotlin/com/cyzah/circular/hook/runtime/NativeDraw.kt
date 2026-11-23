package com.cyzah.circular.hook.runtime

interface NativeDraw {
    @Throws(Throwable::class)
    fun run(): Any?
}
