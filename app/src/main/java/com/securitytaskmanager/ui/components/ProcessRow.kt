package com.securitytaskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.core.Format
import com.securitytaskmanager.core.ProcState
import com.securitytaskmanager.core.ProcessInfo
import com.securitytaskmanager.core.Severity
import com.securitytaskmanager.ui.theme.AccentCyan
import com.securitytaskmanager.ui.theme.DangerRed
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.TextPrimary
import com.securitytaskmanager.ui.theme.TextSecondary
import com.securitytaskmanager.ui.theme.WarningAmber

fun severityColor(s: Severity?): Color = when (s) {
    Severity.HIGH -> DangerRed
    Severity.WARN -> WarningAmber
    Severity.INFO -> AccentCyan
    null -> TextSecondary
}

@Composable
fun ProcessRow(process: ProcessInfo, onClick: () -> Unit) {
    val rowBg = if (process.isCritical) DangerRed.copy(alpha = 0.05f) else Color.Transparent
    val dot = when (process.state) {
        ProcState.RUNNING -> PrimaryGreen
        ProcState.ZOMBIE -> DangerRed
        ProcState.STOPPED -> WarningAmber
        else -> TextSecondary
    }
    val cpuColor = when {
        process.cpuPercent > 50f -> DangerRed
        process.cpuPercent > 20f -> WarningAmber
        else -> TextSecondary
    }
    val sev = process.topSeverity
    val showFlag = sev == Severity.HIGH || sev == Severity.WARN

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = ProcessColumns.HorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColumnLead {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        }

        ColumnCell(ProcessColumns.NAME) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showFlag) {
                    Text("⚠", color = severityColor(sev), fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                }
                Column {
                    Text(
                        text = process.appLabel ?: process.name,
                        color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (process.appLabel != null) {
                        Text(
                            text = process.name, color = TextSecondary, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        MonoCell(process.pid.toString(), ProcessColumns.PID)
        MonoCell(Format.cpu(process.cpuPercent), ProcessColumns.CPU, cpuColor)
        MonoCell(Format.memory(process.rssKb), ProcessColumns.MEM)
        MonoCell(process.user, ProcessColumns.USER, AccentCyan)
        MonoCell(Format.ports(process.ports), ProcessColumns.PORT)
        MonoCell(Format.clock(process.startTimeMs), ProcessColumns.TIME)
    }
}

@Composable
private fun RowScope.MonoCell(text: String, weight: Float, color: Color = TextSecondary) {
    ColumnCell(weight) {
        Text(
            text = text, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
        )
    }
}
