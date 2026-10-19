package com.securitytaskmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF0A0E14)
val SurfaceDark = Color(0xFF111820)
val SurfaceVariant = Color(0xFF1A2333)
val PrimaryGreen = Color(0xFF00FF88)
val AccentCyan = Color(0xFF00CFFF)
val WarningAmber = Color(0xFFFFB300)
val DangerRed = Color(0xFFFF3D5A)
val TextPrimary = Color(0xFFE8F0F8)
val TextSecondary = Color(0xFF8899AA)

private val DarkColors = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = BackgroundDark,
    secondary = AccentCyan,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = DangerRed
)

@Composable
fun StmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
