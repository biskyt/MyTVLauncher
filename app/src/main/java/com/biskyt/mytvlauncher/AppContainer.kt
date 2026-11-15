package com.biskyt.mytvlauncher

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import com.biskyt.mytvlauncher.core.channels.AppChannel
import com.biskyt.mytvlauncher.core.keys.HomePress
import com.biskyt.mytvlauncher.core.keys.HomePressLog
import com.biskyt.mytvlauncher.core.notifications.TvNotification
import com.biskyt.mytvlauncher.core.notifications.TvNotifications
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.AppEntry
import com.biskyt.mytvlauncher.data.AppIcons
import com.biskyt.mytvlauncher.data.AppRepository
import com.biskyt.mytvlauncher.data.BackupFile
import com.biskyt.mytvlauncher.data.BackupRepository
import com.biskyt.mytvlauncher.data.ChannelRepository
import com.biskyt.mytvlauncher.data.ChannelSource
import com.biskyt.mytvlauncher.data.GuideApp
import com.biskyt.mytvlauncher.data.InstalledApps
import com.biskyt.mytvlauncher.data.NetworkMonitor
import com.biskyt.mytvlauncher.data.NotificationSource
import com.biskyt.mytvlauncher.data.RecentsSource
import com.biskyt.mytvlauncher.data.SettingsRepository
import com.biskyt.mytvlauncher.data.SpeakerOutput
import com.biskyt.mytvlauncher.data.SpeakerWake
import com.biskyt.mytvlauncher.data.TvListings
import com.biskyt.mytvlauncher.data.WatchNextRepository
import com.biskyt.mytvlauncher.data.WatchNextSource
import com.biskyt.mytvlauncher.data.db.LauncherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/** The launcher's parts, made once for the app. */
class AppContainer(
    context: Context,
    installed: InstalledApps,
    watchNextSource: WatchNextSource,
    channelSource: ChannelSource,
    val recents: RecentsSource,
    speaker: SpeakerOutput,
    val notificationSource: NotificationSource
) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = LauncherDatabase.open(context)
    val settings = SettingsRepository(context)

    /** The background reads of the installed apps and the TV provider run one at a time. */
    private val reads = Mutex()
    val apps = AppRepository(db.appPlaces(), installed, settings, appScope, reads)
    val icons = AppIcons(context, iconPx(context))
    val listings = TvListings(context)
    val watchNext = WatchNextRepository(db.watchNext(), db.hidden(), watchNextSource, settings, appScope, reads)
    val channels = ChannelRepository(db.channelItems(), db.hidden(), channelSource, settings, appScope, reads)
    val network = NetworkMonitor(context, appScope)
    val speakerWake = SpeakerWake(settings, speaker, appScope)
    val backupFile = BackupFile(context)
    val backup = BackupRepository(db, settings, apps)

    /** Android 9: Backup & restore asked for the storage permission when it opened, once while the launcher runs. */
    @Volatile var backupPermissionAsked = false

    /** A backup file opened with MyTVLauncher from another app, until Backup & restore takes it. */
    val openedFile = MutableStateFlow<Uri?>(null)

    /*
     * Read as the app starts, at the same time, so the first frame usually has them, and kept while the app runs, so
     * Home has them at once when it shows again: null until read.
     */

    /** The accent colour. */
    val accent: StateFlow<Int?> = settings.accent.stateIn(appScope, SharingStarted.Eagerly, null)

    /** Every app, hidden ones too (All apps). */
    val allApps: StateFlow<List<AppEntry>?> = apps.all.stateIn(appScope, SharingStarted.Eagerly, null)

    /** The apps on Home, from the saved list: the apps that are not hidden, in Home's order (from [allApps]). */
    val shownApps: StateFlow<List<AppEntry>?> = allApps.map { all ->
        all?.filter { !it.hidden }
    }.stateIn(appScope, SharingStarted.Eagerly, null)

    /** Every installed app's name, by package name, hidden apps too. */
    val appLabels: StateFlow<Map<String, String>> = allApps.map { all -> all.orEmpty().associate { it.packageName to it.label } }
        .stateIn(appScope, SharingStarted.Eagerly, emptyMap())

    /** Whether Home shows Continue watching (Settings › Home rows). */
    val watchNextShown: StateFlow<Boolean?> = settings.watchNextShown.stateIn(appScope, SharingStarted.Eagerly, null)

    /** Whether Home shows the apps' channel rows (Settings › Home rows). */
    val channelsShown: StateFlow<Boolean?> = settings.channelsShown.stateIn(appScope, SharingStarted.Eagerly, null)

    /** The apps' channel rows, from the saved items (see ChannelRepository). */
    val channelRows: StateFlow<List<AppChannel>?> = channels.channels.stateIn(appScope, SharingStarted.Eagerly, null)

    /** Whether the top bar shows system notifications (Settings › Show notifications). */
    val notificationsShown: StateFlow<Boolean?> = settings.notificationsShown.stateIn(appScope, SharingStarted.Eagerly, null)

    private val access = MutableStateFlow(false)

    /** Whether the user gave notification access: checked while the setting is on (see [checkNotificationAccess]). */
    val notificationAccess: StateFlow<Boolean> = access.asStateFlow()

    /** Checks notification access again: when Home comes to the front with the setting on, and when Settings opens. */
    fun checkNotificationAccess() {
        access.value = notificationSource.allowed()
    }

    /** The notifications the bell counts and its panel lists, newest first: none while the setting is off. */
    val notifications: StateFlow<List<TvNotification>> = combine(notificationsShown, notificationSource.all) { on, all ->
        if (on == true) TvNotifications.shown(all, context.packageName) else emptyList()
    }.stateIn(appScope, SharingStarted.Eagerly, emptyList())

    /** Follows the notifications while the setting is on (see LauncherNotificationListener). */
    fun followNotifications() {
        appScope.launch { notificationsShown.filterNotNull().collect { notificationSource.follow(it) } }
    }

    /** What the TV guide button opens (the TV's own guide until the setting is read). */
    val guideApp: StateFlow<GuideApp> = settings.guideApp.stateIn(appScope, SharingStarted.Eagerly, GuideApp.SYSTEM)

    /** The order of Home's rows, by key (see RowOrder): empty until the user moves one. */
    val rowOrder: StateFlow<List<String>?> = settings.rowOrder.stateIn(appScope, SharingStarted.Eagerly, null)

    /** Continue watching, from the saved items (see WatchNextRepository). */
    val watchNextItems: StateFlow<List<WatchNextItem>?> = watchNext.items.stateIn(appScope, SharingStarted.Eagerly, null)

    private val presses = MutableStateFlow<List<HomePress>>(emptyList())

    /** The Home presses that reached the launcher, newest first (the Home key test in Settings). */
    val homePresses: StateFlow<List<HomePress>> = presses.asStateFlow()

    fun homePressed(inFront: Boolean) {
        val now = SystemClock.uptimeMillis()
        presses.update { HomePressLog.add(it, now, inFront) }
    }

    private companion object {
        /** Home shows icons this wide on its 1280-wide canvas (see LauncherTheme): the whole round app card (`TILE`). */
        const val ICON_UNITS = 84
        const val CANVAS_WIDTH = 1280

        /**
         * The icon's size in pixels on this screen (the canvas spans its width), so each icon is drawn and kept at the
         * size Home shows it.
         */
        fun iconPx(context: Context): Int = context.resources.displayMetrics.widthPixels * ICON_UNITS / CANVAS_WIDTH
    }
}
