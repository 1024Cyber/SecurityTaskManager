package com.securitytaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.data.Prefs
import com.securitytaskmanager.ui.StmViewModel
import com.securitytaskmanager.ui.theme.BackgroundDark
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.SurfaceDark
import com.securitytaskmanager.ui.theme.SurfaceVariant
import com.securitytaskmanager.ui.theme.TextPrimary
import com.securitytaskmanager.ui.theme.TextSecondary

@Composable
fun SettingsScreen(vm: StmViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val ui by vm.ui.collectAsState()

    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        ScreenBar("SETTINGS", onBack)

        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Heading("Refresh rate")
            Prefs.ALLOWED_INTERVALS.forEach { sec ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setInterval(sec) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = ui.intervalSec == sec, onClick = { vm.setInterval(sec) },
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen, unselectedColor = TextSecondary)
                    )
                    Text(if (sec == 1) "Every second" else "Every $sec seconds", color = TextPrimary, fontSize = 14.sp)
                }
            }
            Text(
                "Polling pauses automatically while the app is in the background. " +
                    "Faster rates use more battery because each refresh reads /proc through the root shell.",
                color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )

            Spacer(Modifier.height(22.dp))
            Heading("Display")
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide kernel threads", color = TextPrimary, fontSize = 14.sp)
                    Text("Removes [kworker/…], [kthreadd] and similar from the CORE tab.", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = ui.hideKernelThreads, onCheckedChange = vm::setHideKernelThreads,
                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen, checkedThumbColor = BackgroundDark)
                )
            }

            Spacer(Modifier.height(22.dp))
            Heading("About")
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onAbout).padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("About Security Task Manager", color = TextPrimary, fontSize = 14.sp)
                Text("v${vm.appVersion}", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ScreenBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceDark).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
        }
        Text(title, color = PrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text.uppercase(), color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
    )
}
