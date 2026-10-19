package com.securitytaskmanager.core

enum class ProcessCategory { APP, BACKGROUND, CORE }

enum class ProcState(val code: String) {
    RUNNING("R"), SLEEPING("S"), DISK_SLEEP("D"), ZOMBIE("Z"), STOPPED("T"), IDLE("I"), UNKNOWN("?");

    companion object {
        fun fromChar(c: Char): ProcState = when (c) {
            'R' -> RUNNING
            'S' -> SLEEPING
            'D' -> DISK_SLEEP
            'Z' -> ZOMBIE
            'T', 't' -> STOPPED
            'I' -> IDLE
            else -> UNKNOWN
        }
    }
}

enum class Severity { INFO, WARN, HIGH }

/** A heuristic finding. These are hints to investigate, not verdicts. */
data class SecurityFlag(
    val id: String,
    val severity: Severity,
    val title: String,
    val detail: String
)

data class Connection(
    val protocol: String,        // tcp, tcp6, udp, udp6
    val localAddr: String,
    val localPort: Int,
    val remoteAddr: String,
    val remotePort: Int,
    val state: String,
    val inode: Long
) {
    val isListening: Boolean get() = state == "LISTEN" || (protocol.startsWith("udp") && state == "UNCONN")
    val isWildcardBind: Boolean
        get() = localAddr == "0.0.0.0" || localAddr == "::"
}

data class ProcessInfo(
    val pid: Int,
    val ppid: Int,
    val name: String,
    val packageName: String?,
    val appLabel: String?,
    val uid: Int,
    val user: String,
    val state: ProcState,
    val cpuPercent: Float,           // % of ONE core (like `top`); can exceed 100
    val rssKb: Long,
    val threads: Int,
    val startTimeMs: Long,           // 0 if unknown
    val exePath: String?,
    val connections: List<Connection>,
    val category: ProcessCategory,
    val isKernelThread: Boolean,
    val isCritical: Boolean,
    val flags: List<SecurityFlag>
) {
    /** Listening ports (TCP LISTEN + bound UDP), sorted, unique. */
    val ports: List<Int>
        get() = connections.filter { it.isListening && it.localPort != 0 }
            .map { it.localPort }.distinct().sorted()

    val topSeverity: Severity?
        get() = flags.maxByOrNull { it.severity.ordinal }?.severity
}

data class SystemSummary(
    val cpuPercent: Float,           // whole-device busy %, 0..100
    val cores: Int,
    val memTotalKb: Long,
    val memAvailableKb: Long,
    val swapTotalKb: Long,
    val swapFreeKb: Long,
    val uptimeSec: Double
) {
    val memUsedKb: Long get() = (memTotalKb - memAvailableKb).coerceAtLeast(0)
    val memFraction: Float get() = if (memTotalKb > 0) memUsedKb.toFloat() / memTotalKb else 0f
}

data class Snapshot(
    val takenAtMs: Long,
    val processes: List<ProcessInfo>,
    val summary: SystemSummary
)

enum class SortColumn { NAME, PID, CPU, MEMORY, USER, PORT, TIME }
