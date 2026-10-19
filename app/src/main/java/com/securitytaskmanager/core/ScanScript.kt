package com.securitytaskmanager.core

/**
 * ONE shell invocation per refresh. The old per-file reads meant hundreds of root-shell
 * round trips per refresh; this produces every section in a single pass.
 *
 * Fast sections run every refresh. Slow sections (executables, sockets) are heavier and run
 * every few refreshes; the repository reuses the last result in between.
 */
object ScanScript {

    fun build(includeSlow: Boolean): String {
        val p = ProcParser.SECTION_PREFIX
        val sb = StringBuilder()
        fun section(name: String, cmd: String) {
            sb.append("echo ").append(p).append(name).append('\n')
            sb.append(cmd).append('\n')
        }
        section("UPTIME", "cat /proc/uptime")
        section("CPU", "grep '^cpu' /proc/stat")
        section("MEM", "grep -E '^(MemTotal|MemFree|MemAvailable|SwapTotal|SwapFree):' /proc/meminfo")
        section("PS", "ps -A -o PID,USER,NAME 2>/dev/null")
        section("UID", "grep -H '^Uid:' /proc/[0-9]*/status 2>/dev/null")
        section("STAT", "cat /proc/[0-9]*/stat 2>/dev/null")
        if (includeSlow) {
            section("EXE", "ls -l /proc/[0-9]*/exe 2>/dev/null")
            section("TCP", "cat /proc/net/tcp 2>/dev/null")
            section("TCP6", "cat /proc/net/tcp6 2>/dev/null")
            section("UDP", "cat /proc/net/udp 2>/dev/null")
            section("UDP6", "cat /proc/net/udp6 2>/dev/null")
            section("FD", "ls -l /proc/[0-9]*/fd 2>/dev/null | grep -E '^/proc|socket:'")
        }
        sb.append("echo ").append(p).append("END\n")
        return sb.toString()
    }
}
