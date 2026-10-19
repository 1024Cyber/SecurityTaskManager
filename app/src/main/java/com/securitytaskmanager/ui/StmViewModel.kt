package com.securitytaskmanager.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.securitytaskmanager.core.ProcessInfo
import com.securitytaskmanager.core.Snapshot
import com.securitytaskmanager.core.SortColumn
import com.securitytaskmanager.data.ExportKind
import com.securitytaskmanager.data.Prefs
import com.securitytaskmanager.data.ProcessRepository
import com.securitytaskmanager.data.RootShell
import com.securitytaskmanager.data.SnapshotWriter
import com.securitytaskmanager.domain.KillOutcome
import com.securitytaskmanager.domain.KillProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val rootChecked: Boolean = false,
    val isRooted: Boolean = false,
    val started: Boolean = false,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val snapshot: Snapshot? = null,
    val query: String = "",
    val sort: SortColumn = SortColumn.NAME,
    val ascending: Boolean = true,
    val intervalSec: Int = 3,
    val hideKernelThreads: Boolean = true,
    val selected: ProcessInfo? = null,
    val selectedGone: Boolean = false,
    val killing: Boolean = false,
    val message: String? = null
)

class StmViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val repo = ProcessRepository(app)
    private val killer = KillProcess()

    private val _ui = MutableStateFlow(
        UiState(intervalSec = prefs.intervalSec, hideKernelThreads = prefs.hideKernelThreads)
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val active = MutableStateFlow(true)
    private var pollJob: Job? = null
    private var lastError: String? = null

    val appVersion: String = try {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    // ── start-up ────────────────────────────────────────────────────────────
    suspend fun checkRoot(): Boolean {
        val rooted = RootShell.isRoot()
        _ui.update { it.copy(rootChecked = true, isRooted = rooted) }
        return rooted
    }

    fun start() {
        if (_ui.value.started) return
        _ui.update { it.copy(started = true) }
        startPolling()
    }

    /** Called from the lifecycle: no root-shell work while the app is in the background. */
    fun setActive(isActive: Boolean) { active.value = isActive }

    // ── polling / refresh ───────────────────────────────────────────────────
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                active.first { it }
                scanOnce(force = false)
                delay(_ui.value.intervalSec * 1000L)
            }
        }
    }

    private suspend fun scanOnce(force: Boolean) {
        try {
            val snap = repo.scan(force)
            lastError = null
            _ui.update { applySnapshot(it, snap) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = "Scan failed: ${e.message ?: e::class.java.simpleName}"
            _ui.update { it.copy(loading = false, message = if (msg != lastError) msg else it.message) }
            lastError = msg
        }
    }

    private fun applySnapshot(s: UiState, snap: Snapshot): UiState {
        val sel = s.selected
        val fresh = sel?.let { old -> snap.processes.firstOrNull { it.pid == old.pid && it.name == old.name } }
        return s.copy(
            snapshot = snap, loading = false,
            selected = fresh ?: sel,                 // keep the last known data once it has exited
            selectedGone = sel != null && fresh == null
        )
    }

    /** Pull-to-refresh. Also forces the slower executable/socket scan. */
    fun refresh() {
        if (_ui.value.refreshing) return
        viewModelScope.launch {
            _ui.update { it.copy(refreshing = true) }
            try { scanOnce(force = true) } finally { _ui.update { it.copy(refreshing = false) } }
        }
    }

    // ── table state ─────────────────────────────────────────────────────────
    fun setQuery(q: String) = _ui.update { it.copy(query = q) }

    fun setSort(col: SortColumn) = _ui.update {
        it.copy(sort = col, ascending = if (it.sort == col) !it.ascending else true)
    }

    fun select(p: ProcessInfo?) = _ui.update { it.copy(selected = p, selectedGone = false) }

    // ── settings ────────────────────────────────────────────────────────────
    fun setInterval(sec: Int) {
        prefs.intervalSec = sec
        _ui.update { it.copy(intervalSec = sec) }
        if (_ui.value.started) startPolling()
    }

    fun setHideKernelThreads(hide: Boolean) {
        prefs.hideKernelThreads = hide
        _ui.update { it.copy(hideKernelThreads = hide) }
    }

    // ── actions ─────────────────────────────────────────────────────────────
    fun killRefusal(p: ProcessInfo): String? = killer.refusal(p)

    fun kill(p: ProcessInfo) {
        if (_ui.value.killing) return
        viewModelScope.launch {
            _ui.update { it.copy(killing = true) }
            val label = p.appLabel ?: p.name
            val msg = when (val r = killer.kill(p)) {
                is KillOutcome.Killed -> "Killed $label (PID ${p.pid})"
                is KillOutcome.Restarted ->
                    "$label was killed but Android started it again (PID ${r.newPid ?: "?"})"
                is KillOutcome.Failed -> "Could not kill $label: ${r.reason}"
                is KillOutcome.Refused -> r.reason
            }
            _ui.update { it.copy(killing = false, message = msg, selected = null) }
            scanOnce(force = true)
        }
    }

    fun export(uri: Uri, kind: ExportKind) {
        val snap = _ui.value.snapshot
        if (snap == null) {
            _ui.update { it.copy(message = "Nothing to export yet") }
            return
        }
        viewModelScope.launch {
            val msg = try {
                SnapshotWriter.write(getApplication(), uri, kind, snap, appVersion)
                "Exported ${snap.processes.size} processes (${kind.ext.uppercase()})"
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
            _ui.update { it.copy(message = msg) }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
}
