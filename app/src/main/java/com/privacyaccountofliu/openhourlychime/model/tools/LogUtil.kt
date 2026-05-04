package com.privacyaccountofliu.openhourlychime.model.tools

import android.util.Log

object LogUtil {
    private const val TAG = "HourlyChime"
    private var isDebugEnabled = false

    fun init(enabled: Boolean) {
        isDebugEnabled = enabled
    }

    fun d(tag: String = TAG, message: String) {
        if (isDebugEnabled) Log.d(tag, message)
    }

    fun i(tag: String = TAG, message: String) {
        if (isDebugEnabled) Log.i(tag, message)
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
