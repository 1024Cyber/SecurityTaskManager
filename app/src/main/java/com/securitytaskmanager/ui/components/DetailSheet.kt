package com.securitytaskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.core.Format
import com.securitytaskmanager.core.ProcessInfo
import com.securitytaskmanager.ui.theme.AccentCyan
import com.securitytaskmanager.ui.theme.BackgroundDark
import com.securitytaskmanager.ui.theme.DangerRed
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.SurfaceDark
import com.securitytaskmanager.ui.theme.SurfaceVariant
import com.securitytaskmanager.ui.theme.TextPrimary
import com.securitytaskmanager.ui.theme.TextSecondary
import com.securitytaskmanager.ui.theme.WarningAmber

private const val MAX_CONNECTIONS_SHOWN = 40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    process: ProcessInfo,
    gone: Boolean,
    canKill: Boolean,
    killing: Boolean,
    killRefusal: String?,
    onKill: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimary
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                process.appLabel ?: process.name,
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            if (process.appLabel != null) {
                Text(process.name, color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            if (gone) {
                Spacer(Modifier.height(6.dp))
                Text("This process has exited. Showing its last known data.", color = WarningAmber, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))

            Line("PID", process.pid.toString())
            Line("Parent PID", process.ppid.toString())
            Line("State", "${process.state.code}  (${process.state.name.lowercase().replace('_', ' ')})")
            Line("User", "${process.user}  (uid ${process.uid})")
            Line("Category", process.category.name)
            process.packageName?.let { Line("Package", it) }
            Line("CPU", Format.cpu(process.cpuPercent) + " of one core")
            Line("Memory (RSS)", Format.memory(process.rssKb))
            Line("Threads", process.threads.toString())
            Line("Started", Format.dateTime(process.startTimeMs))
            Line("Executable", process.exePath ?: "unknown (needs root)")
            Line("Listening ports", if (process.ports.isEmpty()) "none" else process.ports.joinToString(", "))

            if (process.flags.isNotEmpty()) {
                Section("Security findings")
                Text(
                    "Heuristic hints to investigate, not proof of anything malicious.",
                    color = TextSecondary, fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                process.flags.forEach { f ->
                    val c = severityColor(f.severity)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.copy(alpha = 0.10f))
                            .padding(10.dp)
                    ) {
                        Text("${f.severity.name}  ·  ${f.title}", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(f.detail, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }

            if (process.connections.isNotEmpty()) {
                Section("Network (${process.connections.size})")
                process.connections.take(MAX_CONNECTIONS_SHOWN).forEach { c ->
                    Text(
                        "${c.protocol.uppercase()}  ${c.localAddr}:${c.localPort} → ${c.remoteAddr}:${c.remotePort}",
                        color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                    )
                    Text(c.state, color = AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                }
                if (process.connections.size > MAX_CONNECTIONS_SHOWN) {
                    Text("… and ${process.connections.size - MAX_CONNECTIONS_SHOWN} more", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { confirm = true },
                enabled = canKill && !gone && !killing && killRefusal == null,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (killing) {
                    CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.width(18.dp).height(18.dp))
                } else {
                    Text(if (canKill) "Kill process" else "Kill process (root required)")
                }
            }
            val hint = killRefusal ?: if (!canKill) "Root access is required to kill processes." else null
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Text(hint, color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    if (process.isCritical) "Critical system process" else "Kill process?",
                    color = if (process.isCritical) DangerRed else TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (process.isCritical) {
                        Text(
                            "Killing ${process.name} can freeze the device, soft-reboot it, or log you out.",
                            color = DangerRed, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Kill ${process.appLabel ?: process.name} (PID ${process.pid})?", color = TextPrimary, fontSize = 14.sp)
                    if (process.packageName != null && !process.isCritical) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The whole app will be force-stopped so Android doesn't restart it right away.",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirm = false; onKill() }) {
                    Text("Kill", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(18.dp))
    Text(title.uppercase(), color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
    }
}
