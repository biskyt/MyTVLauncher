package com.biskyt.mytvlauncher

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.biskyt.mytvlauncher.data.AudioSpeaker
import com.biskyt.mytvlauncher.data.ChannelSource
import com.biskyt.mytvlauncher.data.InstalledApps
import com.biskyt.mytvlauncher.data.NotificationSource
import com.biskyt.mytvlauncher.data.PackageManagerApps
import com.biskyt.mytvlauncher.data.RecentsSource
import com.biskyt.mytvlauncher.data.SpeakerOutput
import com.biskyt.mytvlauncher.data.SystemNotifications
import com.biskyt.mytvlauncher.data.UsageStatsRecents
import com.biskyt.mytvlauncher.data.WatchNextSource
import com.biskyt.mytvlauncher.data.channelSourceFor
import com.biskyt.mytvlauncher.data.watchNextSourceFor

open class LauncherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container =
            AppContainer(this, installedApps(), watchNextSource(), channelSource(), recentsSource(), speakerOutput(), notificationSource())
        // A look at the installed apps as the launcher starts, then one for each change (see AppRepository).
        locales = resources.configuration.locales
        container.apps.watch(this)
        container.apps.refresh()
        container.watchNext.start()
        container.channels.start()
        container.speakerWake.start(this) { container.homeRowsNudge.nudge() }
        container.followNotifications()
    }

    private var locales: LocaleList? = null

    // The apps' names follow the language: a look when it changes, not for other changes (screen, keyboard).
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.locales == locales) return
        locales = newConfig.locales
        container.apps.refresh()
    }

    /** Where the installed apps come from: the package manager (a fake one in tests). */
    protected open fun installedApps(): InstalledApps = PackageManagerApps(this)

    /** Where Watch Next items come from: the TV provider (a fake one in tests). */
    protected open fun watchNextSource(): WatchNextSource = watchNextSourceFor(this)

    /** Where the apps' channels come from: the TV provider (a fake one in tests). */
    protected open fun channelSource(): ChannelSource = channelSourceFor(this)

    /** Where the recent apps come from: Android's usage stats (a fake in tests). */
    protected open fun recentsSource(): RecentsSource = UsageStatsRecents(this)

    /** The sound output that speaker wake uses: Android's audio (a fake in tests). */
    protected open fun speakerOutput(): SpeakerOutput = AudioSpeaker(this)

    /** Where system notifications come from: Android's notification listener (a fake in tests). */
    protected open fun notificationSource(): NotificationSource = SystemNotifications(this)
}

val Context.container: AppContainer get() = (applicationContext as LauncherApp).container
