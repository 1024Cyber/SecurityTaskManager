package com.securitytaskmanager.core

import org.junit.Assert.*
import org.junit.Test

class ProcParserTest {

    @Test fun statLineSimple() {
        val l = "4321 (bash) S 1 4321 4321 34816 4321 4194304 100 0 0 0 150 50 0 0 20 0 3 0 9000 12345678 2048 18446744073709551615 0 0 0 0 0 0 0 0 0 0 0 0 17 0 0 0"
        val s = ProcParser.parseStatLine(l)!!
        assertEquals(4321, s.pid); assertEquals("bash", s.comm); assertEquals('S', s.state)
        assertEquals(1, s.ppid); assertEquals(150L, s.utime); assertEquals(50L, s.stime)
        assertEquals(200L, s.cpuTicks); assertEquals(3, s.threads)
        assertEquals(9000L, s.startTicks); assertEquals(2048L, s.rssPages)
    }

    @Test fun statLineWithSpacesAndParensInComm() {
        val l = "77 (Binder:77_1 (x)) R 2 0 0 0 0 0 0 0 0 0 10 5 0 0 20 0 9 0 500 0 7 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"
        val s = ProcParser.parseStatLine(l)!!
        assertEquals("Binder:77_1 (x)", s.comm); assertEquals('R', s.state); assertEquals(2, s.ppid)
        assertEquals(15L, s.cpuTicks); assertEquals(9, s.threads); assertEquals(500L, s.startTicks); assertEquals(7L, s.rssPages)
    }

    @Test fun statLineGarbageReturnsNull() {
        assertNull(ProcParser.parseStatLine(""))
        assertNull(ProcParser.parseStatLine("garbage"))
        assertNull(ProcParser.parseStatLine("12 (x) S 1 2"))
        assertNull(ProcParser.parseStatLine("abc (x) S " + "1 ".repeat(30)))
    }

    @Test fun sections() {
        val m = ProcParser.splitSections(listOf("noise", "@@STM:A", "a1", "a2", "@@STM:B", "b1", "@@STM:END"))
        assertEquals(listOf("a1", "a2"), m["A"]); assertEquals(listOf("b1"), m["B"])
        assertTrue(m["END"]!!.isEmpty())
    }

    @Test fun uids() {
        val m = ProcParser.parseUids(listOf("/proc/1/status:Uid:\t0\t0\t0\t0", "/proc/812/status:Uid:\t10123\t10123\t10123\t10123", "junk"))
        assertEquals(0, m[1]); assertEquals(10123, m[812]); assertEquals(2, m.size)
    }

    @Test fun psSkipsHeaderAndKeepsNameWithSpaces() {
        val m = ProcParser.parsePs(listOf("PID USER NAME", "1 root init", "812 u0_a123 com.foo.bar:remote", "55 root [kworker/0:1H]", "9 system my daemon name"))
        assertEquals(PsEntry("root", "init"), m[1]); assertEquals(PsEntry("u0_a123", "com.foo.bar:remote"), m[812])
        assertEquals("[kworker/0:1H]", m[55]!!.name); assertEquals("my daemon name", m[9]!!.name)
        assertEquals(4, m.size)
    }

    @Test fun exe() {
        val m = ProcParser.parseExe(listOf(
            "lrwxrwxrwx 1 root root 0 2026-10-19 12:00 /proc/1/exe -> /system/bin/init",
            "lrwxrwxrwx 1 root root 0 2026-10-19 12:00 /proc/900/exe -> /data/local/tmp/frida-server (deleted)"))
        assertEquals("/system/bin/init", m[1]); assertEquals("/data/local/tmp/frida-server (deleted)", m[900])
    }

    @Test fun socketOwners() {
        val m = ProcParser.parseSocketOwners(listOf(
            "/proc/10/fd:", "lrwx------ 1 root root 64 2026-10-19 12:00 3 -> socket:[11111]",
            "/proc/20/fd:", "lrwx------ 1 u0_a1 u0_a1 64 2026-10-19 12:00 7 -> socket:[22222]",
            "lrwx------ 1 u0_a1 u0_a1 64 2026-10-19 12:00 8 -> socket:[33333]"))
        assertEquals(10, m[11111L]); assertEquals(20, m[22222L]); assertEquals(20, m[33333L])
    }

    @Test fun uptimeCpuMem() {
        assertEquals(1234.56, ProcParser.parseUptime(listOf("1234.56 9999.00")), 0.001)
        val c = ProcParser.parseCpuTimes(listOf("cpu  100 0 50 800 50 0 0 0 0 0", "cpu0 50 0 25 400 25 0 0 0 0 0", "cpu1 50 0 25 400 25 0 0 0 0 0"))!!
        assertEquals(1000L, c.totalTicks); assertEquals(850L, c.idleTicks); assertEquals(2, c.cores)
        val m = ProcParser.parseMemInfo(listOf("MemTotal:        4000000 kB", "MemFree:          100000 kB", "MemAvailable:    1500000 kB", "SwapTotal:       2000000 kB", "SwapFree:        1000000 kB"))
        assertEquals(4000000L, m.totalKb); assertEquals(1500000L, m.availableKb); assertEquals(1000000L, m.swapFreeKb)
    }

    @Test fun memAvailableFallsBackToFree() {
        assertEquals(100L, ProcParser.parseMemInfo(listOf("MemTotal: 400 kB", "MemFree: 100 kB")).availableKb)
    }
}
