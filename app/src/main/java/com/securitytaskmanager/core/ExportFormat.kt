package com.securitytaskmanager.core

import com.google.gson.GsonBuilder

/** Pure serialisation of a snapshot to JSON / CSV text. Writing to a file lives in data/. */
object ExportFormat {

    fun toJson(s: Snapshot, appVersion: String): String {
        val doc = linkedMapOf<String, Any?>(
            "app" to "Security Task Manager",
            "version" to appVersion,
            "taken_at" to Format.dateTime(s.takenAtMs),
            "summary" to linkedMapOf(
                "processes" to s.processes.size,
                "cpu_percent" to s.summary.cpuPercent,
                "cores" to s.summary.cores,
                "mem_total_kb" to s.summary.memTotalKb,
                "mem_available_kb" to s.summary.memAvailableKb,
                "uptime_sec" to s.summary.uptimeSec
            ),
            "processes" to s.processes.map { p ->
                linkedMapOf(
                    "pid" to p.pid, "ppid" to p.ppid, "name" to p.name, "package" to p.packageName,
                    "category" to p.category.name, "user" to p.user, "uid" to p.uid,
                    "state" to p.state.code, "cpu_percent" to p.cpuPercent, "rss_kb" to p.rssKb,
                    "threads" to p.threads, "start_time" to Format.dateTime(p.startTimeMs),
                    "exe" to p.exePath, "listening_ports" to p.ports,
                    "connections" to p.connections.map {
                        "${it.protocol} ${it.localAddr}:${it.localPort} -> ${it.remoteAddr}:${it.remotePort} ${it.state}"
                    },
                    "flags" to p.flags.map { "${it.severity}: ${it.title}" }
                )
            }
        )
        return GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(doc)
    }

    fun toCsv(s: Snapshot): String {
        val sb = StringBuilder()
        sb.append("pid,name,package,category,user,uid,state,cpu_percent,rss_kb,threads,start_time,ports,exe,flags\n")
        for (p in s.processes) {
            sb.append(
                listOf(
                    p.pid.toString(), p.name, p.packageName.orEmpty(), p.category.name, p.user,
                    p.uid.toString(), p.state.code, "%.1f".format(java.util.Locale.US, p.cpuPercent),
                    p.rssKb.toString(), p.threads.toString(), Format.dateTime(p.startTimeMs),
                    p.ports.joinToString(" "), p.exePath.orEmpty(),
                    p.flags.joinToString("; ") { "${it.severity}: ${it.title}" }
                ).joinToString(",") { csv(it) }
            ).append('\n')
        }
        return sb.toString()
    }

    /** RFC 4180 quoting, plus a leading-quote guard against spreadsheet formula injection. */
    fun csv(field: String): String {
        var f = field
        if (f.isNotEmpty() && f[0] in "=+-@\t\r") f = "'$f"
        return if (f.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + f.replace("\"", "\"\"") + "\"" else f
    }
}
