package com.securitytaskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.core.Format
import com.securitytaskmanager.core.SystemSummary
import com.securitytaskmanager.ui.theme.AccentCyan
import com.securitytaskmanager.ui.theme.DangerRed
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.SurfaceDark
import com.securitytaskmanager.ui.theme.SurfaceVariant
import com.securitytaskmanager.ui.theme.TextSecondary
import com.securitytaskmanager.ui.theme.WarningAmber

/** Task Manager-style CPU and memory strip. */
@Composable
fun SummaryBar(summary: SystemSummary?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val cpu = summary?.cpuPercent ?: 0f
        Meter(
            label = "CPU", value = if (summary == null) "—" else "${cpu.toInt()}%  ·  ${summary.cores} cores",
            fraction = cpu / 100f, modifier = Modifier.weight(1f)
        )
        Meter(
            label = "MEM",
            value = if (summary == null) "—" else "${Format.memory(summary.memUsedKb)} / ${Format.memory(summary.memTotalKb)}",
            fraction = summary?.memFraction ?: 0f, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Meter(label: String, value: String, fraction: Float, modifier: Modifier = Modifier) {
    val f = fraction.coerceIn(0f, 1f)
    val color: Color = when {
        f > 0.85f -> DangerRed
        f > 0.65f -> WarningAmber
        else -> PrimaryGreen
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 3.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp)).background(SurfaceVariant)
        ) {
            Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(color))
        }
    }
}
