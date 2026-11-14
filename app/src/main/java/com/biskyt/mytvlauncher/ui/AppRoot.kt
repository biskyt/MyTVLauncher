package com.biskyt.mytvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.ui.common.KeepsFocus
import com.biskyt.mytvlauncher.ui.home.AllAppsScreen
import com.biskyt.mytvlauncher.ui.home.HomeScreen
import com.biskyt.mytvlauncher.ui.home.RecentsScreen
import com.biskyt.mytvlauncher.ui.settings.AccentScreen
import com.biskyt.mytvlauncher.ui.settings.CheckScreen
import com.biskyt.mytvlauncher.ui.settings.HomeRowsScreen
import com.biskyt.mytvlauncher.ui.settings.SettingsScreen
import com.biskyt.mytvlauncher.ui.theme.LauncherTheme
import com.biskyt.mytvlauncher.ui.theme.Palette

/** The launcher's screens, in its theme. Back closes the top screen, and does nothing on Home. */
@Composable
fun AppRoot(nav: Navigator) {
    val accent by LocalContext.current.container.accent.collectAsState()
    Box(Modifier.fillMaxSize().background(Palette.background)) {
        // Until the accent colour is read (a few ms after start, and usually before the first frame): the background.
        val argb = accent ?: return@Box
        LauncherTheme(argb) {
            BackHandler { nav.back() }
            // Each screen keeps its state (its scroll, the item used last) while a screen over it is open.
            val saved = rememberSaveableStateHolder()
            val screen = nav.top
            saved.SaveableStateProvider(screen.name) {
                KeepsFocus(screen) {
                    when (screen) {
                        Screen.HOME -> HomeScreen(nav)
                        Screen.SETTINGS -> SettingsScreen(nav)
                        Screen.ACCENT -> AccentScreen()
                        Screen.HOME_ROWS -> HomeRowsScreen()
                        Screen.ALL_APPS -> AllAppsScreen()
                        Screen.RECENTS -> RecentsScreen()
                        Screen.CHECK -> CheckScreen()
                    }
                }
            }
        }
    }
}
