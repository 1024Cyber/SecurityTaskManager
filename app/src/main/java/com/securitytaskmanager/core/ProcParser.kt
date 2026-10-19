package com.securitytaskmanager.core

/** Tick-count fields from /proc/<pid>/stat. */
data class RawStat(
    val pid: Int,
    val comm: String,
    val state: Char,
    val ppid: Int,
    val utime: Long,
    val stime: Long,
    val threads: Int,
    val startTicks: Long,
    val rssPages: Long
) {
    val cpuTicks: Long get() = utime + stime
}

data class PsEntry(val user: String, val name: String)

data class CpuTimes(val totalTicks: Long, val idleTicks: Long, val cores: Int)

data class MemInfo(
    val totalKb: Long, val availableKb: Long, val swapTotalKb: Long, val swapFreeKb: Long
)

/**
 * Pure parsers for the output of [ScanScript]. No Android imports, so they run in
 * plain JVM unit tests.
 */
object ProcParser {

    const val SECTION_PREFIX = "@@STM:"
    const val PAGE_SIZE_KB = 4L
    const val CLK_TCK = 100.0

    /** Splits the batched script output into named sections. */
    fun splitSections(lines: List<String>): Map<String, List<String>> {
        val out = LinkedHashMap<String, MutableList<String>>()
        var current: MutableList<String>? = null
        for (line in lines) {
            if (line.startsWith(SECTION_PREFIX)) {
                val name = line.removePrefix(SECTION_PREFIX).trim()
                current = out.getOrPut(name) { mutableListOf() }
            } else {
                current?.add(line)
            }
        }
        return out
    }

    /**
     * "123 (com.foo (x)) S 1 ..." — comm may contain spaces and parentheses, so the
     * split point is the LAST ')'. Field n (1-based, per proc(5)) is at after[n-3].
     */
    fun parseStatLine(line: String): RawStat? {
        val open = line.indexOf('(')
        val close = line.lastIndexOf(')')
        if (open <= 0 || close < open) return null
        val pid = line.substring(0, open).trim().toIntOrNull() ?: return null
        val comm = line.substring(open + 1, close)
        val after = line.substring(close + 1).trim().split(' ')
        if (after.size < 22) return null
        return try {
            RawStat(
                pid = pid,
                comm = comm,
                state = after[0].firstOrNull() ?: '?',
                ppid = after[1].toInt(),
                utime = after[11].toLong(),
                stime = after[12].toLong(),
                threads = after[17].toInt(),
                startTicks = after[19].toLong(),
                rssPages = after[21].toLong()
            )
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** `grep -H '^Uid:' /proc/[0-9]*\/status` -> pid to real uid. */
    private val UID_RE = Regex("""^/proc/(\d+)/status:Uid:\s+(\d+)""")

    fun parseUids(lines: List<String>): Map<Int, Int> {
        val m = HashMap<Int, Int>()
        for (l in lines) {
            val r = UID_RE.find(l) ?: continue
            m[r.groupValues[1].toInt()] = r.groupValues[2].toInt()
        }
        return m
    }

    /** `ps -A -o PID,USER,NAME` -> pid to (user, name). Header line is skipped. */
    fun parsePs(lines: List<String>): Map<Int, PsEntry> {
        val m = HashMap<Int, PsEntry>()
        for (l in lines) {
            val parts = l.trim().split(Regex("\\s+"), limit = 3)
            if (parts.size < 3) continue
            val pid = parts[0].toIntOrNull() ?: continue   // skips "PID USER NAME" header
            m[pid] = PsEntry(user = parts[1], name = parts[2].trim())
        }
        return m
    }

    private val EXE_RE = Regex("""/proc/(\d+)/exe -> (.+)$""")

    fun parseExe(lines: List<String>): Map<Int, String> {
        val m = HashMap<Int, String>()
        for (l in lines) {
            val r = EXE_RE.find(l) ?: continue
            m[r.groupValues[1].toInt()] = r.groupValues[2].trim()
        }
        return m
    }

    private val FD_HEADER_RE = Regex("""^/proc/(\d+)/fd/?:""")
    private val SOCKET_RE = Regex("""socket:\[(\d+)]""")

    /** Output of `ls -l /proc/[0-9]*\/fd | grep ...` -> socket inode to pid. */
    fun parseSocketOwners(lines: List<String>): Map<Long, Int> {
        val m = HashMap<Long, Int>()
        var pid = -1
        for (l in lines) {
            val h = FD_HEADER_RE.find(l)
            if (h != null) {
                pid = h.groupValues[1].toInt()
                continue
            }
            if (pid < 0) continue
            val s = SOCKET_RE.find(l) ?: continue
            m[s.groupValues[1].toLong()] = pid
        }
        return m
    }

    fun parseUptime(lines: List<String>): Double =
        lines.firstOrNull()?.trim()?.split(' ')?.firstOrNull()?.toDoubleOrNull() ?: 0.0

    /** Aggregate "cpu " line plus a count of "cpuN" lines. */
    fun parseCpuTimes(lines: List<String>): CpuTimes? {
        var total = -1L
        var idle = 0L
        var cores = 0
        for (l in lines) {
            val p = l.trim().split(Regex("\\s+"))
            if (p.isEmpty()) continue
            if (p[0] == "cpu") {
                val v = p.drop(1).mapNotNull { it.toLongOrNull() }
                if (v.size < 4) continue
                // user nice system idle iowait irq softirq steal
                total = v.take(8).sum()
                idle = v[3] + (v.getOrNull(4) ?: 0L)
            } else if (p[0].startsWith("cpu")) {
                cores++
            }
        }
        return if (total >= 0) CpuTimes(total, idle, cores.coerceAtLeast(1)) else null
    }

    fun parseMemInfo(lines: List<String>): MemInfo {
        val m = HashMap<String, Long>()
        for (l in lines) {
            val i = l.indexOf(':')
            if (i < 0) continue
            val v = l.substring(i + 1).trim().split(' ').firstOrNull()?.toLongOrNull() ?: continue
            m[l.substring(0, i)] = v
        }
        val total = m["MemTotal"] ?: 0L
        val avail = m["MemAvailable"] ?: ((m["MemFree"] ?: 0L))
        return MemInfo(total, avail, m["SwapTotal"] ?: 0L, m["SwapFree"] ?: 0L)
    }
}
