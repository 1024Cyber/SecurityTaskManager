package com.securitytaskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.securitytaskmanager.ui.StmViewModel
import com.securitytaskmanager.ui.screens.AboutScreen
import com.securitytaskmanager.ui.screens.MainScreen
import com.securitytaskmanager.ui.screens.SettingsScreen
import com.securitytaskmanager.ui.screens.SplashScreen
import com.securitytaskmanager.ui.theme.StmTheme

private enum class Screen { Splash, Main, Settings, About }

class MainActivity : ComponentActivity() {

    private val vm: StmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StmTheme {
                var screen by rememberSaveable {
                    mutableStateOf(if (vm.ui.value.started) Screen.Main else Screen.Splash)
                }
                when (screen) {
                    Screen.Splash -> SplashScreen(vm) { screen = Screen.Main }
                    Screen.Main -> MainScreen(vm, onOpenSettings = { screen = Screen.Settings })
                    Screen.Settings -> {
                        BackHandler { screen = Screen.Main }
                        SettingsScreen(vm, onBack = { screen = Screen.Main }, onAbout = { screen = Screen.About })
                    }
                    Screen.About -> {
                        BackHandler { screen = Screen.Settings }
                        AboutScreen(vm.appVersion, onBack = { screen = Screen.Settings })
                    }
                }
            }
        }
    }

    // No root-shell polling while the app isn't visible.
    override fun onStart() { super.onStart(); vm.setActive(true) }
    override fun onStop() { vm.setActive(false); super.onStop() }
}
