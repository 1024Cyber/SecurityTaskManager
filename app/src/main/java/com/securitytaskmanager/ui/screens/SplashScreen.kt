package com.securitytaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securitytaskmanager.ui.StmViewModel
import com.securitytaskmanager.ui.theme.BackgroundDark
import com.securitytaskmanager.ui.theme.PrimaryGreen
import com.securitytaskmanager.ui.theme.TextSecondary
import com.securitytaskmanager.ui.theme.WarningAmber
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(vm: StmViewModel, onFinished: () -> Unit) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        val rooted = vm.checkRoot()
        // keep the result on screen long enough to read it
        val minShow = if (rooted) 1400L else 2200L
        val left = minShow - (System.currentTimeMillis() - t0)
        if (left > 0) delay(left)
        vm.start()
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("STM", color = PrimaryGreen, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text("SECURITY TASK MANAGER", color = TextSecondary, fontSize = 12.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(40.dp))

            if (!ui.rootChecked) {
                CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(14.dp))
                Text("Checking root access…", color = TextSecondary, fontSize = 13.sp)
            } else if (ui.isRooted) {
                Text("ROOT ACCESS GRANTED", color = PrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text("Full mode: every process is visible", color = TextSecondary, fontSize = 12.sp)
            } else {
                Text("NO ROOT ACCESS", color = WarningAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Limited mode: Android only shows this app its own processes. Grant root for the full list.",
                    color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}
