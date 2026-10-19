package com.securitytaskmanager.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    fun memory(kb: Long): String = when {
        kb >= 1024L * 1024L -> String.format(Locale.US, "%.1fG", kb / 1024.0 / 1024.0)
        kb >= 1024L -> "${kb / 1024}M"
        else -> "${kb}K"
    }

    fun cpu(percent: Float): String =
        if (percent >= 100f) "${percent.toInt()}%" else String.format(Locale.US, "%.1f%%", percent)

    fun clock(ms: Long): String =
        if (ms <= 0) "—" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

    fun dateTime(ms: Long): String =
        if (ms <= 0) "unknown" else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))

    fun duration(sec: Double): String {
        val s = sec.toLong()
        val d = s / 86400
        val h = (s % 86400) / 3600
        val m = (s % 3600) / 60
        return if (d > 0) "${d}d ${h}h" else if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun ports(ports: List<Int>): String = when {
        ports.isEmpty() -> "—"
        ports.size <= 2 -> ports.joinToString(",")
        else -> "${ports[0]},${ports[1]}+"
    }
}
