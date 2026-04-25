package org.movzx.dumbai.util

import android.util.Log

object Logger {
    var debugEnabled: Boolean = false

    fun d(tag: String, message: String) {
        if (debugEnabled) Log.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (debugEnabled) Log.e(tag, message, throwable)
    }

    fun w(tag: String, message: String) {
        if (debugEnabled) Log.w(tag, message)
    }

    fun v(tag: String, message: String) {
        if (debugEnabled) Log.v(tag, message)
    }

    fun i(tag: String, message: String) {
        if (debugEnabled) Log.i(tag, message)
    }
}
