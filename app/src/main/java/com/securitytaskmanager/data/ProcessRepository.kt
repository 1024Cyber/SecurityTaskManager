package com.securitytaskmanager.data

import android.content.Context
import com.securitytaskmanager.core.AppIndex
import com.securitytaskmanager.core.ProcParser
import com.securitytaskmanager.core.ScanScript
import com.securitytaskmanager.core.ScanState
import com.securitytaskmanager.core.Snapshot
import com.securitytaskmanager.core.SnapshotBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Runs scans and keeps the state needed for CPU deltas. Scans never overlap (mutex), so a
 * pull-to-refresh during a poll just waits its turn.
 */
class ProcessRepository(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var state: ScanState? = null
    private var apps: AppIndex = AppIndex.EMPTY
    private var appsLoadedAt = 0L
    private var scanCount = 0

    /** Executables + sockets are heavier, so they run every [SLOW_EVERY]th scan (or when forced). */
    suspend fun scan(forceSlow: Boolean = false): Snapshot = mutex.withLock {
        val slow = forceSlow || state == null || scanCount % SLOW_EVERY == 0
        scanCount++

        val now = System.currentTimeMillis()
        if (now - appsLoadedAt > APP_INDEX_TTL_MS || apps === AppIndex.EMPTY) {
            apps = withContext(Dispatchers.IO) { AppIndexLoader.load(appContext) }
            appsLoadedAt = now
        }

        val lines = RootShell.run(ScanScript.build(includeSlow = slow))
        val raw = ProcParser.splitSections(lines)
        if (raw["STAT"].isNullOrEmpty()) {
            throw IOException("Shell returned no process data (is /proc readable?)")
        }
        val (snapshot, newState) = withContext(Dispatchers.Default) {
            SnapshotBuilder.build(raw, state, apps, System.currentTimeMillis())
        }
        state = newState
        snapshot
    }

    companion object {
        private const val SLOW_EVERY = 3
        private const val APP_INDEX_TTL_MS = 60_000L
    }
}
