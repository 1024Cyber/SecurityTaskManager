package com.securitytaskmanager.core

/** Carried from one scan to the next: CPU deltas and the last slow-section results. */
data class ScanState(
    val ticksByPid: Map<Int, Long>,
    val uptimeSec: Double,
    val cpu: CpuTimes?,
    val exeByPid: Map<Int, String>,
    val connectionsByPid: Map<Int, List<Connection>>
)

/** Pure: raw script sections in, [Snapshot] out. */
object SnapshotBuilder {

    fun build(
        raw: Map<String, List<String>>,
        prev: ScanState?,
        apps: AppIndex,
        nowMs: Long
    ): Pair<Snapshot, ScanState> {
        val uptime = ProcParser.parseUptime(raw["UPTIME"].orEmpty())
        val cpuNow = ProcParser.parseCpuTimes(raw["CPU"].orEmpty())
        val mem = ProcParser.parseMemInfo(raw["MEM"].orEmpty())
        val ps = ProcParser.parsePs(raw["PS"].orEmpty())
        val uids = ProcParser.parseUids(raw["UID"].orEmpty())
        val stats = raw["STAT"].orEmpty().mapNotNull { ProcParser.parseStatLine(it) }

        // Slow sections: use fresh data if this scan ran them, else carry the previous result.
        val hasSlow = raw.containsKey("EXE") || raw.containsKey("FD")
        val exeMap = if (hasSlow) ProcParser.parseExe(raw["EXE"].orEmpty()) else prev?.exeByPid.orEmpty()
        val connsByPid: Map<Int, List<Connection>> = if (hasSlow) {
            val owners = ProcParser.parseSocketOwners(raw["FD"].orEmpty())
            val all = NetParser.parse("tcp", raw["TCP"].orEmpty()) +
                NetParser.parse("tcp6", raw["TCP6"].orEmpty()) +
                NetParser.parse("udp", raw["UDP"].orEmpty()) +
                NetParser.parse("udp6", raw["UDP6"].orEmpty())
            all.mapNotNull { c -> owners[c.inode]?.let { it to c } }
                .groupBy({ it.first }, { it.second })
        } else prev?.connectionsByPid.orEmpty()

        val dtSec = if (prev != null && prev.uptimeSec > 0) uptime - prev.uptimeSec else 0.0
        val bootMs = nowMs - (uptime * 1000).toLong()

        val procs = stats.map { s ->
            val psEntry = ps[s.pid]
            val uid = uids[s.pid] ?: -1
            val rawName = psEntry?.name?.takeIf { it.isNotBlank() } ?: s.comm
            val name = Categorizer.baseName(rawName)
            val user = psEntry?.user ?: Categorizer.userName(uid)
            val kernel = Categorizer.isKernelThread(s.pid, s.ppid, rawName)
            val critical = Categorizer.isCritical(s.pid, name, kernel)
            val pkg = Categorizer.packageFor(name, uid, apps)
            val category = Categorizer.categorize(uid, name, pkg, kernel, critical, apps)
            val state = ProcState.fromChar(s.state)
            val conns = connsByPid[s.pid].orEmpty()
            val exe = exeMap[s.pid]

            val cpu = if (dtSec > 0) {
                val prevTicks = prev?.ticksByPid?.get(s.pid)
                if (prevTicks != null && s.cpuTicks >= prevTicks)
                    ((s.cpuTicks - prevTicks) / dtSec).toFloat().coerceAtLeast(0f)   // ticks/sec == % of one core
                else 0f
            } else 0f

            ProcessInfo(
                pid = s.pid, ppid = s.ppid, name = name,
                packageName = pkg, appLabel = pkg?.let { apps.byPackage[it]?.label },
                uid = uid, user = user, state = state,
                cpuPercent = cpu,
                rssKb = s.rssPages * ProcParser.PAGE_SIZE_KB,
                threads = s.threads,
                startTimeMs = if (uptime > 0) bootMs + (s.startTicks * 1000.0 / ProcParser.CLK_TCK).toLong() else 0L,
                exePath = exe,
                connections = conns,
                category = category,
                isKernelThread = kernel,
                isCritical = critical,
                flags = SecurityAnalyzer.analyze(exe, conns, state, category, critical)
            )
        }

        val cpuPct = if (prev?.cpu != null && cpuNow != null) {
            val dTotal = (cpuNow.totalTicks - prev.cpu.totalTicks).toDouble()
            val dIdle = (cpuNow.idleTicks - prev.cpu.idleTicks).toDouble()
            if (dTotal > 0) ((1.0 - dIdle / dTotal) * 100.0).toFloat().coerceIn(0f, 100f) else 0f
        } else 0f

        val summary = SystemSummary(
            cpuPercent = cpuPct,
            cores = cpuNow?.cores ?: 1,
            memTotalKb = mem.totalKb, memAvailableKb = mem.availableKb,
            swapTotalKb = mem.swapTotalKb, swapFreeKb = mem.swapFreeKb,
            uptimeSec = uptime
        )
        val state = ScanState(
            ticksByPid = stats.associate { it.pid to it.cpuTicks },
            uptimeSec = uptime, cpu = cpuNow,
            exeByPid = exeMap, connectionsByPid = connsByPid
        )
        return Snapshot(nowMs, procs, summary) to state
    }
}
