package com.securitytaskmanager.core

/**
 * Cheap, explainable heuristics. They point at things worth a look; they do not
 * prove anything is malicious (frida-server, Termux, adb and dev tools all trip them).
 */
object SecurityAnalyzer {

    private val WRITABLE_EXEC_PREFIXES = listOf(
        "/data/local/tmp", "/sdcard", "/storage/", "/data/media", "/mnt/sdcard", "/mnt/shell", "/dev/shm", "/tmp"
    )
    private val APP_DIR_EXEC_PREFIXES = listOf("/data/data/", "/data/user/", "/data/app/")

    fun analyze(
        exePath: String?,
        connections: List<Connection>,
        state: ProcState,
        category: ProcessCategory,
        isCritical: Boolean
    ): List<SecurityFlag> {
        val flags = ArrayList<SecurityFlag>()

        if (exePath != null) {
            if (exePath.endsWith("(deleted)")) {
                flags.add(
                    SecurityFlag(
                        "exe_deleted", Severity.HIGH, "Binary deleted while running",
                        "The executable $exePath no longer exists on disk. Malware often does this to hide; " +
                            "an in-place app update can also cause it."
                    )
                )
            }
            val clean = exePath.removeSuffix(" (deleted)")
            if (WRITABLE_EXEC_PREFIXES.any { clean.startsWith(it) }) {
                flags.add(
                    SecurityFlag(
                        "exe_writable_path", Severity.HIGH, "Runs from a writable location",
                        "Executable is at $clean, a place any shell/app user can write to. " +
                            "Legitimate on dev devices (e.g. frida-server); unusual otherwise."
                    )
                )
            } else if (APP_DIR_EXEC_PREFIXES.any { clean.startsWith(it) }) {
                flags.add(
                    SecurityFlag(
                        "exe_app_dir", Severity.WARN, "Native binary from an app directory",
                        "Executable is at $clean. Normal for terminals like Termux; worth checking for anything else."
                    )
                )
            }
        }

        val exposed = connections.filter { it.state == "LISTEN" && it.isWildcardBind }
        if (exposed.isNotEmpty() && !isCritical) {
            val ports = exposed.map { it.localPort }.distinct().sorted().joinToString(", ")
            flags.add(
                SecurityFlag(
                    "listen_all", if (category == ProcessCategory.CORE) Severity.INFO else Severity.WARN,
                    "Listening on all interfaces",
                    "Accepts TCP connections on every network interface (port $ports), so other devices " +
                        "on the same network can reach it."
                )
            )
        }

        if (state == ProcState.ZOMBIE) {
            flags.add(
                SecurityFlag(
                    "zombie", Severity.INFO, "Zombie process",
                    "Exited, but its parent has not collected the exit status yet."
                )
            )
        }
        return flags
    }
}
