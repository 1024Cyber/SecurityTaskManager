package com.securitytaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.ui.theme.AccentCyan
import com.securitytaskmanager.ui.theme.BackgroundDark
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.TextPrimary
import com.securitytaskmanager.ui.theme.TextSecondary

@Composable
fun AboutScreen(version: String, onBack: () -> Unit) {
    val uri = LocalUriHandler.current

    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        ScreenBar("ABOUT", onBack)

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STM", color = PrimaryGreen, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text("Security Task Manager", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Version $version", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

            Spacer(Modifier.height(20.dp))
            Text(
                "A Task Manager-style process monitor for Android. Read-only by default; with root it shows " +
                    "every process, its ports and executable, flags suspicious behaviour, and can stop a process.",
                color = TextSecondary, fontSize = 13.sp
            )

            Spacer(Modifier.height(28.dp))
            Text(AboutInfo.AUTHOR_NAME, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(AboutInfo.AUTHOR_TAGLINE, color = TextSecondary, fontSize = 13.sp)

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { uri.openUri(AboutInfo.GITHUB_URL) }, modifier = Modifier.fillMaxWidth()) {
                Text("GitHub", color = AccentCyan)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { uri.openUri(AboutInfo.X_URL) }, modifier = Modifier.fillMaxWidth()) {
                Text("X / Twitter", color = AccentCyan)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { uri.openUri(AboutInfo.REPO_URL) }, modifier = Modifier.fillMaxWidth()) {
                Text("Source code & releases", color = AccentCyan)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Use only on devices you own or are authorised to test. Killing system processes can " +
                    "freeze or reboot a device.",
                color = TextSecondary, fontSize = 11.sp
            )
        }
    }
}
