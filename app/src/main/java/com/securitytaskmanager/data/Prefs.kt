package com.securitytaskmanager.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("stm_prefs", Context.MODE_PRIVATE)

    var intervalSec: Int
        get() = sp.getInt("interval_sec", 3).takeIf { it in ALLOWED_INTERVALS } ?: 3
        set(v) = sp.edit().putInt("interval_sec", v).apply()

    var hideKernelThreads: Boolean
        get() = sp.getBoolean("hide_kernel_threads", true)
        set(v) = sp.edit().putBoolean("hide_kernel_threads", v).apply()

    companion object {
        val ALLOWED_INTERVALS = listOf(1, 3, 5)
    }
}
