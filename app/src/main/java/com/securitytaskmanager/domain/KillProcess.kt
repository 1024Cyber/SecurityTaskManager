package com.securitytaskmanager.domain

import com.securitytaskmanager.core.ProcessInfo
import com.securitytaskmanager.data.RootShell

sealed class KillOutcome {
    object Killed : KillOutcome()
    /** The process died but Android (or its own supervisor) started it again. */
    data class Restarted(val newPid: Int?) : KillOutcome()
    data class Failed(val reason: String) : KillOutcome()
    data class Refused(val reason: String) : KillOutcome()
}

/**
 * Why a bare `kill -9` "doesn't work" on Android apps: ActivityManager sees the death and
 * restarts sticky services. `am force-stop` goes through ActivityManager instead, so it also
 * cancels services/alarms/jobs and marks the package stopped. So: force-stop first (apps only),
 * then kill -9 whatever is left, then CHECK, and report what really happened.
 */
class KillProcess(private val ownPid: Int = android.os.Process.myPid()) {

    private val safeToken = Regex("^[A-Za-z0-9_.@-]+$")

    fun refusal(p: ProcessInfo): String? = when {
        p.pid <= 1 -> "PID ${p.pid} (init) can't be killed."
        p.isKernelThread -> "Kernel threads can't be killed from user space."
        p.pid == ownPid -> "That's Security Task Manager itself."
        else -> null
    }

    suspend fun kill(p: ProcessInfo): KillOutcome {
        refusal(p)?.let { return KillOutcome.Refused(it) }

        val pkg = p.packageName?.takeIf { !p.isCritical && p.uid >= 10000 && safeToken.matches(it) }
        val target = pkg ?: p.name.takeIf { safeToken.matches(it) }

        val script = buildString {
            if (pkg != null) append("am force-stop ").append(pkg).append(" 2>&1\n")
            append("kill -9 ").append(p.pid).append(" 2>&1\n")
            append("sleep 0.8\n")
            append("if [ -d /proc/").append(p.pid).append(" ]; then echo @@ALIVE=1; else echo @@ALIVE=0; fi\n")
            if (target != null) append("echo @@PIDOF=\$(pidof ").append(target).append(" 2>/dev/null)\n")
        }

        return try {
            val out = RootShell.run(script)
            val alive = out.any { it.trim() == "@@ALIVE=1" }
            val others = out.firstOrNull { it.startsWith("@@PIDOF=") }
                ?.removePrefix("@@PIDOF=")?.trim()?.split(' ')?.mapNotNull { it.toIntOrNull() }
                ?.filter { it != p.pid }.orEmpty()
            val errors = out.filter { !it.startsWith("@@") && it.isNotBlank() }.joinToString("; ")

            when {
                alive -> KillOutcome.Failed(
                    errors.ifBlank { "Process did not exit (protected by the system, or in uninterruptible sleep)." }
                )
                others.isNotEmpty() -> KillOutcome.Restarted(others.first())
                else -> KillOutcome.Killed
            }
        } catch (e: Exception) {
            KillOutcome.Failed(e.message ?: "Unknown error")
        }
    }
}
