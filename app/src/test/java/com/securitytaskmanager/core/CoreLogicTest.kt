package com.securitytaskmanager.core

import org.junit.Assert.*
import org.junit.Test

class CoreLogicTest {

    private val apps = AppIndex(
        uidToPackages = mapOf(
            10123 to listOf("com.user.game"), 10050 to listOf("com.samsung.android.dialer"),
            10060 to listOf("com.android.chrome"), 10070 to listOf("com.vendor.preload")
        ),
        byPackage = mapOf(
            "com.user.game" to AppMeta("com.user.game", "Cool Game", false, true),
            "com.samsung.android.dialer" to AppMeta("com.samsung.android.dialer", "Phone", true, true),
            "com.android.chrome" to AppMeta("com.android.chrome", "Chrome", true, true),
            "com.vendor.preload" to AppMeta("com.vendor.preload", "Preload", true, false)
        )
    )

    private fun cat(uid: Int, name: String, ppid: Int = 1, pid: Int = 500): ProcessCategory {
        val pkg = Categorizer.packageFor(name, uid, apps)
        val k = Categorizer.isKernelThread(pid, ppid, name)
        val crit = Categorizer.isCritical(pid, name, k)
        return Categorizer.categorize(uid, name, pkg, k, crit, apps)
    }

    @Test fun categories() {
        assertEquals(ProcessCategory.APP, cat(10123, "com.user.game"))
        assertEquals(ProcessCategory.APP, cat(10123, "com.user.game:service"))
        assertEquals(ProcessCategory.APP, cat(10060, "com.android.chrome"))          // allow-listed
        assertEquals(ProcessCategory.BACKGROUND, cat(10050, "com.samsung.android.dialer")) // OEM
        assertEquals(ProcessCategory.BACKGROUND, cat(10070, "com.vendor.preload"))   // system, no launcher
        assertEquals(ProcessCategory.CORE, cat(0, "vold"))
        assertEquals(ProcessCategory.CORE, cat(1000, "system_server"))               // critical name
        assertEquals(ProcessCategory.BACKGROUND, cat(1001, "rild"))                  // radio daemon
        assertEquals(ProcessCategory.CORE, cat(0, "[kworker/0:1]", ppid = 2))        // kernel thread
        assertEquals(ProcessCategory.BACKGROUND, cat(-1, "mystery"))
    }

    @Test fun nameAndUserHelpers() {
        assertEquals("vold", Categorizer.baseName("/system/bin/vold"))
        assertEquals("com.foo:x", Categorizer.baseName("com.foo:x"))
        assertEquals("u0_a123", Categorizer.userName(10123)); assertEquals("u10_a5", Categorizer.userName(1010005))
        assertEquals("system", Categorizer.userName(1000)); assertEquals("root", Categorizer.userName(0))
    }

    @Test fun securityFlags() {
        val listen = Connection("tcp", "0.0.0.0", 4444, "0.0.0.0", 0, "LISTEN", 1)
        val local = Connection("tcp", "127.0.0.1", 4444, "0.0.0.0", 0, "LISTEN", 2)
        assertTrue(SecurityAnalyzer.analyze("/data/local/tmp/x", emptyList(), ProcState.SLEEPING, ProcessCategory.BACKGROUND, false).any { it.id == "exe_writable_path" && it.severity == Severity.HIGH })
        assertTrue(SecurityAnalyzer.analyze("/data/local/tmp/x (deleted)", emptyList(), ProcState.SLEEPING, ProcessCategory.BACKGROUND, false).any { it.id == "exe_deleted" })
        assertTrue(SecurityAnalyzer.analyze("/data/data/com.termux/files/usr/bin/bash", emptyList(), ProcState.SLEEPING, ProcessCategory.APP, false).any { it.id == "exe_app_dir" })
        assertTrue(SecurityAnalyzer.analyze("/system/bin/sh", emptyList(), ProcState.SLEEPING, ProcessCategory.APP, false).isEmpty())
        assertEquals(Severity.WARN, SecurityAnalyzer.analyze(null, listOf(listen), ProcState.SLEEPING, ProcessCategory.APP, false).single().severity)
        assertEquals(Severity.INFO, SecurityAnalyzer.analyze(null, listOf(listen), ProcState.SLEEPING, ProcessCategory.CORE, false).single().severity)
        assertTrue(SecurityAnalyzer.analyze(null, listOf(local), ProcState.SLEEPING, ProcessCategory.APP, false).isEmpty())
        assertTrue(SecurityAnalyzer.analyze(null, listOf(listen), ProcState.SLEEPING, ProcessCategory.CORE, true).isEmpty())  // critical daemons skipped
        assertEquals("zombie", SecurityAnalyzer.analyze(null, emptyList(), ProcState.ZOMBIE, ProcessCategory.APP, false).single().id)
    }

    // ---- SnapshotBuilder end-to-end on a fake script output ------------------------------------
    private fun stat(pid: Int, comm: String, st: Char, ppid: Int, ut: Long, stt: Long, thr: Int, start: Long, rss: Long) =
        "$pid ($comm) $st $ppid 0 0 0 0 0 0 0 0 0 $ut $stt 0 0 20 0 $thr 0 $start 0 $rss 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"

    private fun raw(uptime: Double, utGame: Long, cpuTotal: Long, cpuIdle: Long, slow: Boolean): Map<String, List<String>> {
        val m = linkedMapOf(
            "UPTIME" to listOf("$uptime 5000.0"),
            "CPU" to listOf("cpu  ${cpuTotal - cpuIdle} 0 0 $cpuIdle 0 0 0 0 0 0", "cpu0 1 0 0 1 0 0 0 0 0 0", "cpu1 1 0 0 1 0 0 0 0 0 0"),
            "MEM" to listOf("MemTotal: 4000000 kB", "MemAvailable: 1000000 kB", "SwapTotal: 0 kB", "SwapFree: 0 kB"),
            "PS" to listOf("PID USER NAME", "1 root init", "2 root [kthreadd]", "812 u0_a123 com.user.game", "900 shell frida-server"),
            "UID" to listOf("/proc/1/status:Uid:\t0\t0\t0\t0", "/proc/2/status:Uid:\t0\t0\t0\t0", "/proc/812/status:Uid:\t10123\t10123\t10123\t10123", "/proc/900/status:Uid:\t2000\t2000\t2000\t2000"),
            "STAT" to listOf(
                stat(1, "init", 'S', 0, 10, 5, 1, 1, 100), stat(2, "kthreadd", 'S', 0, 0, 0, 1, 1, 0),
                stat(812, "droid.game", 'R', 300, utGame, 0, 20, 500, 25600), stat(900, "frida-server", 'S', 1, 5, 5, 4, 900, 1000))
        )
        if (slow) {
            m["EXE"] = listOf("lrwxrwxrwx 1 root root 0 d t /proc/1/exe -> /system/bin/init", "lrwxrwxrwx 1 shell shell 0 d t /proc/900/exe -> /data/local/tmp/frida-server")
            m["TCP"] = listOf("  sl", "   0: 00000000:6A1A 00000000:0000 0A 0:0 0:0 0 2000 0 5001 1 0")
            m["TCP6"] = emptyList(); m["UDP"] = emptyList(); m["UDP6"] = emptyList()
            m["FD"] = listOf("/proc/900/fd:", "lrwx------ 1 shell shell 64 d t 3 -> socket:[5001]")
        }
        return m
    }

    @Test fun builderFirstScanThenDelta() {
        val (s1, st1) = SnapshotBuilder.build(raw(1000.0, 100, 10000, 8000, true), null, apps, 1_700_000_000_000)
        assertEquals(4, s1.processes.size)
        val game = s1.processes.single { it.pid == 812 }
        assertEquals(ProcessCategory.APP, game.category); assertEquals("com.user.game", game.packageName); assertEquals("Cool Game", game.appLabel)
        assertEquals(25600L * 4, game.rssKb); assertEquals(0f, game.cpuPercent, 0f)                // no delta yet
        assertEquals(1_700_000_000_000 - 1_000_000 + 5000, game.startTimeMs)                       // boot + 500 ticks
        assertEquals(ProcessCategory.CORE, s1.processes.single { it.pid == 2 }.category)
        assertTrue(s1.processes.single { it.pid == 2 }.isKernelThread)
        val frida = s1.processes.single { it.pid == 900 }
        assertEquals(listOf(27162), frida.ports); assertEquals("/data/local/tmp/frida-server", frida.exePath)
        assertTrue(frida.flags.any { it.id == "exe_writable_path" }); assertTrue(frida.flags.any { it.id == "listen_all" })
        assertEquals(Severity.HIGH, frida.topSeverity)

        // second scan: 2s later, game used 200 more ticks = 100 ticks/s = 100% of one core; fast scan only
        val (s2, _) = SnapshotBuilder.build(raw(1002.0, 300, 10400, 8200, false), st1, apps, 1_700_000_002_000)
        val g2 = s2.processes.single { it.pid == 812 }
        assertEquals(100f, g2.cpuPercent, 0.01f)
        assertEquals("slow data carried over", listOf(27162), s2.processes.single { it.pid == 900 }.ports)
        assertEquals("/data/local/tmp/frida-server", s2.processes.single { it.pid == 900 }.exePath)
        // device busy: total +400, idle +200 -> 50 %
        assertEquals(50f, s2.summary.cpuPercent, 0.01f); assertEquals(2, s2.summary.cores)
        assertEquals(0.75f, s2.summary.memFraction, 0.001f)
    }

    @Test fun builderSurvivesEmptyOutput() {
        val (s, st) = SnapshotBuilder.build(emptyMap(), null, AppIndex.EMPTY, 1L)
        assertTrue(s.processes.isEmpty()); assertNull(st.cpu)
    }

    @Test fun filterSort() {
        val (s, _) = SnapshotBuilder.build(raw(1000.0, 100, 10000, 8000, true), null, apps, 1_700_000_000_000)
        val all = s.processes
        assertEquals(listOf(812), ListFilter.apply(all, ProcessCategory.APP, "", SortColumn.NAME, true, true).map { it.pid })
        assertEquals(listOf(1), ListFilter.apply(all, ProcessCategory.CORE, "", SortColumn.NAME, true, true).map { it.pid })   // kthreadd hidden
        assertEquals(listOf(2, 1), ListFilter.apply(all, ProcessCategory.CORE, "", SortColumn.PID, false, false).map { it.pid })
        assertEquals(listOf(812), ListFilter.apply(all, ProcessCategory.APP, "cool game", SortColumn.NAME, true, true).map { it.pid })
        assertEquals(listOf(900), ListFilter.apply(all, ProcessCategory.BACKGROUND, "27162", SortColumn.NAME, true, true).map { it.pid })
        assertEquals(1, ListFilter.count(all, ProcessCategory.CORE, true))
    }

    @Test fun formatHelpers() {
        assertEquals("512K", Format.memory(512)); assertEquals("100M", Format.memory(102400)); assertEquals("1.5G", Format.memory(1572864))
        assertEquals("3.5%", Format.cpu(3.5f)); assertEquals("250%", Format.cpu(250f))
        assertEquals("—", Format.ports(emptyList())); assertEquals("80,443", Format.ports(listOf(80, 443))); assertEquals("1,2+", Format.ports(listOf(1, 2, 3)))
        assertEquals("1d 2h", Format.duration(93600.0)); assertEquals("5m", Format.duration(300.0))
    }

    @Test fun exportCsvEscaping() {
        assertEquals("plain", ExportFormat.csv("plain")); assertEquals("\"a,b\"", ExportFormat.csv("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", ExportFormat.csv("say \"hi\"")); assertEquals("'=SUM(A1)", ExportFormat.csv("=SUM(A1)"))
        val (s, _) = SnapshotBuilder.build(raw(1000.0, 100, 10000, 8000, true), null, apps, 1_700_000_000_000)
        val csv = ExportFormat.toCsv(s).lines().filter { it.isNotBlank() }
        assertEquals(5, csv.size); assertTrue(csv[0].startsWith("pid,name"))
        val json = ExportFormat.toJson(s, "2.0.0")
        assertTrue(json.contains("\"frida-server\"")); assertTrue(json.contains("\"processes\""))
    }
}
