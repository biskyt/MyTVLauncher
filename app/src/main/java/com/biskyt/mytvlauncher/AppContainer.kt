package com.biskyt.mytvlauncher

import android.content.Context
import android.os.SystemClock
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.core.keys.HomePress
import com.biskyt.mytvlauncher.core.keys.HomePressLog
import com.biskyt.mytvlauncher.data.AppIcons
import com.biskyt.mytvlauncher.data.AppRepository
import com.biskyt.mytvlauncher.data.InstalledApps
import com.biskyt.mytvlauncher.data.SettingsRepository
import com.biskyt.mytvlauncher.data.TvListings
import com.biskyt.mytvlauncher.data.db.LauncherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** The launcher's parts, made once for the app. */
class AppContainer(context: Context, installed: InstalledApps) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = LauncherDatabase.open(context)
    val settings = SettingsRepository(context)
    val apps = AppRepository(db.appPlaces(), installed, appScope)
    val icons = AppIcons(context, iconPx(context))
    val listings = TvListings(context)

    /*
     * Read as the app starts, at the same time, so the first frame usually has them, and kept while the app runs, so
     * Home has them at once when it shows again: null until read.
     */

    /** The accent colour. */
    val accent: StateFlow<Int?> = settings.accent.stateIn(appScope, SharingStarted.Eagerly, null)

    /** The apps on Home, from the saved list. */
    val shownApps: StateFlow<List<InstalledApp>?> = apps.shown.stateIn(appScope, SharingStarted.Eagerly, null)

    private val presses = MutableStateFlow<List<HomePress>>(emptyList())

    /** The Home presses that reached the launcher, newest first (the Home key test in Settings). */
    val homePresses: StateFlow<List<HomePress>> = presses.asStateFlow()

    fun homePressed(inFront: Boolean) {
        val now = SystemClock.uptimeMillis()
        presses.update { HomePressLog.add(it, now, inFront) }
    }

    private companion object {
        /** Home shows icons this wide on its 1280-wide canvas (see LauncherTheme). */
        const val ICON_UNITS = 56
        const val CANVAS_WIDTH = 1280

        /**
         * The icon's size in pixels on this screen (the canvas spans its width), so each icon is drawn and kept at the
         * size Home shows it.
         */
        fun iconPx(context: Context): Int = context.resources.displayMetrics.widthPixels * ICON_UNITS / CANVAS_WIDTH
    }
}
