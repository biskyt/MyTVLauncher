package com.biskyt.mytvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.ui.common.KeepsFocus
import com.biskyt.mytvlauncher.ui.home.AllAppsScreen
import com.biskyt.mytvlauncher.ui.home.HomeScreen
import com.biskyt.mytvlauncher.ui.home.KeepInViewSpec
import com.biskyt.mytvlauncher.ui.home.RecentsScreen
import com.biskyt.mytvlauncher.ui.home.rememberRowPadPx
import com.biskyt.mytvlauncher.ui.settings.AccentScreen
import com.biskyt.mytvlauncher.ui.settings.BackupScreen
import com.biskyt.mytvlauncher.ui.settings.HomeRowsScreen
import com.biskyt.mytvlauncher.ui.settings.RowOrderScreen
import com.biskyt.mytvlauncher.ui.settings.SettingsScreen
import com.biskyt.mytvlauncher.ui.settings.SpeakerWakeScreen
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
            // Lists scroll only as far as they must to show the item with focus (see KeepInViewSpec).
            val margin = rememberRowPadPx()
            val spec = remember(margin) { KeepInViewSpec(margin) }
            @OptIn(ExperimentalFoundationApi::class)
            CompositionLocalProvider(LocalBringIntoViewSpec provides spec) { Screens(nav) }
            // A backup file opened with the launcher from another app: Backup & restore offers it.
            val opened by LocalContext.current.container.openedFile.collectAsState()
            LaunchedEffect(opened) { if (opened != null && nav.top != Screen.BACKUP) nav.open(Screen.BACKUP) }
        }
    }
}

@Composable
private fun Screens(nav: Navigator) {
    // Each screen keeps its state (its scroll, the item used last) while a screen over it is open.
    val saved = rememberSaveableStateHolder()
    val screen = nav.top
    saved.SaveableStateProvider(screen.name) {
        KeepsFocus(screen) {
            when (screen) {
                Screen.HOME -> HomeScreen(nav)
                Screen.SETTINGS -> SettingsScreen(nav)
                Screen.ACCENT -> AccentScreen()
                Screen.HOME_ROWS -> HomeRowsScreen(nav)
                Screen.ROW_ORDER -> RowOrderScreen()
                Screen.BACKUP -> BackupScreen()
                Screen.SPEAKER_WAKE -> SpeakerWakeScreen()
                Screen.ALL_APPS -> AllAppsScreen()
                Screen.RECENTS -> RecentsScreen(nav)
            }
        }
    }
}
