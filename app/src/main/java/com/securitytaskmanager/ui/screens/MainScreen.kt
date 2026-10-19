package com.securitytaskmanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.core.ListFilter
import com.securitytaskmanager.core.ProcessCategory
import com.securitytaskmanager.data.ExportKind
import com.securitytaskmanager.data.SnapshotWriter
import com.securitytaskmanager.ui.StmViewModel
import com.securitytaskmanager.ui.components.DetailSheet
import com.securitytaskmanager.ui.components.SummaryBar
import com.securitytaskmanager.ui.theme.AccentCyan
import com.securitytaskmanager.ui.theme.BackgroundDark
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.SurfaceDark
import com.securitytaskmanager.ui.theme.SurfaceVariant
import com.securitytaskmanager.ui.theme.TextPrimary
import com.securitytaskmanager.ui.theme.TextSecondary
import com.securitytaskmanager.ui.theme.WarningAmber

private data class TabSpec(val label: String, val icon: ImageVector, val category: ProcessCategory)

private val TABS = listOf(
    TabSpec("APPS", Icons.Default.Apps, ProcessCategory.APP),
    TabSpec("BACKGROUND", Icons.Default.SettingsSuggest, ProcessCategory.BACKGROUND),
    TabSpec("CORE", Icons.Default.Memory, ProcessCategory.CORE)
)

@Composable
fun MainScreen(vm: StmViewModel, onOpenSettings: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showExport by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // One launcher per format (CreateDocument fixes the MIME type when it is created).
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportKind.JSON.mime)
    ) { uri -> if (uri != null) vm.export(uri, ExportKind.JSON) }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportKind.CSV.mime)
    ) { uri -> if (uri != null) vm.export(uri, ExportKind.CSV) }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Long)
            vm.consumeMessage()
        }
    }

    val snapshot = ui.snapshot
    val all = snapshot?.processes.orEmpty()
    val category = TABS[tab].category
    val rows = remember(snapshot, category, ui.query, ui.sort, ui.ascending, ui.hideKernelThreads) {
        ListFilter.apply(all, category, ui.query, ui.sort, ui.ascending, ui.hideKernelThreads)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = BackgroundDark
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(BackgroundDark)) {

            // ── Top bar ─────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().background(SurfaceDark).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "SECURITY TASK MANAGER", color = PrimaryGreen, fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "${all.size} processes  ·  " + if (ui.isRooted) "ROOT" else "LIMITED",
                        color = if (ui.isRooted) TextSecondary else WarningAmber, fontSize = 11.sp
                    )
                }
                Row {
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export snapshot", tint = AccentCyan, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            SummaryBar(snapshot?.summary)

            if (!ui.isRooted) {
                Text(
                    "Limited mode: without root, Android only lets this app see its own processes.",
                    color = WarningAmber, fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().background(WarningAmber.copy(alpha = 0.10f)).padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // ── Search ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search name, package, user, PID, port…", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Close, "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen, unfocusedBorderColor = SurfaceVariant,
                    focusedContainerColor = SurfaceDark, unfocusedContainerColor = SurfaceDark,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = PrimaryGreen
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // ── Tabs ────────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().background(SurfaceDark)) {
                TABS.forEachIndexed { i, t ->
                    val selected = i == tab
                    val locked = t.category == ProcessCategory.CORE && !ui.isRooted
                    val color = when {
                        locked -> TextSecondary.copy(alpha = 0.4f)
                        selected -> PrimaryGreen
                        else -> TextSecondary
                    }
                    val count = ListFilter.count(all, t.category, ui.hideKernelThreads)
                    Column(
                        Modifier.weight(1f).clickable { tab = i }.padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(t.icon, null, tint = color, modifier = Modifier.size(14.dp))
                            Text(t.label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                            if (snapshot != null && !locked) Text("$count", color = color.copy(alpha = 0.7f), fontSize = 10.sp)
                            if (locked) Icon(Icons.Default.Lock, "Locked", tint = color, modifier = Modifier.size(10.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(2.dp).background(if (selected) PrimaryGreen else Color.Transparent))
                    }
                }
            }

            // ── Table ───────────────────────────────────────────────────────
            ProcessListContent(
                rows = rows,
                loading = ui.loading,
                refreshing = ui.refreshing,
                lockedForNonRoot = category == ProcessCategory.CORE && !ui.isRooted,
                sort = ui.sort,
                ascending = ui.ascending,
                emptyHint = if (ui.query.isNotBlank()) "No process matches \"${ui.query}\"" else "No processes in this tab",
                onSort = vm::setSort,
                onRefresh = vm::refresh,
                onSelect = vm::select
            )
        }
    }

    // ── Detail sheet ────────────────────────────────────────────────────────
    ui.selected?.let { p ->
        DetailSheet(
            process = p,
            gone = ui.selectedGone,
            canKill = ui.isRooted,
            killing = ui.killing,
            killRefusal = vm.killRefusal(p),
            onKill = { vm.kill(p) },
            onDismiss = { vm.select(null) }
        )
    }

    // ── Export dialog ───────────────────────────────────────────────────────
    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            containerColor = SurfaceDark,
            title = { Text("Export snapshot", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Saves all ${all.size} processes (every tab) to a file you choose, e.g. in Downloads.",
                    color = TextSecondary, fontSize = 13.sp
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showExport = false; jsonLauncher.launch(SnapshotWriter.suggestedName(ExportKind.JSON))
                    }) { Text("JSON", color = AccentCyan) }
                    TextButton(onClick = {
                        showExport = false; csvLauncher.launch(SnapshotWriter.suggestedName(ExportKind.CSV))
                    }) { Text("CSV", color = PrimaryGreen) }
                }
            },
            dismissButton = { TextButton(onClick = { showExport = false }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}
