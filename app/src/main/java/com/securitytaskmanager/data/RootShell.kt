package com.securitytaskmanager.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin libsu wrapper. Everything blocking is moved onto Dispatchers.IO here. */
object RootShell {

    /** First call may show the su prompt. Returns false if the shell fell back to non-root. */
    suspend fun isRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }
    }

    /** Runs a (multi-line) script in the persistent shell and returns stdout lines. */
    suspend fun run(script: String): List<String> = withContext(Dispatchers.IO) {
        Shell.cmd(script).exec().out
    }
}
