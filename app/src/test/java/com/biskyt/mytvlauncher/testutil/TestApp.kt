package com.biskyt.mytvlauncher.testutil

import android.os.SystemClock
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.core.channels.AppChannel
import com.biskyt.mytvlauncher.core.recents.AppUse
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.ChannelSource
import com.biskyt.mytvlauncher.data.InstalledApps
import com.biskyt.mytvlauncher.data.RecentsSource
import com.biskyt.mytvlauncher.data.WatchNextSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.robolectric.shadows.ShadowLooper

/** The real app wiring, with the installed apps from [FakeApps]. */
class TestApp : LauncherApp() {
    override fun onCreate() {
        lookUpNioClasses() // before any image is loaded
        super.onCreate()
    }

    override fun installedApps(): InstalledApps = InstalledApps { FakeApps.list() }

    override fun watchNextSource(): WatchNextSource = FakeWatchNext

    override fun recentsSource(): RecentsSource = FakeRecents

    override fun channelSource(): ChannelSource = FakeChannels

    /**
     * Robolectric calls this after each test. Each test gets a new app, but the coroutine threads are shared by the
     * whole run: work left running would hold up the next test. Then the settings go back to their defaults, as the
     * settings file outlives the app.
     */
    override fun onTerminate() {
        try {
            stopMainThreadEffects()
            runMainLooperDown()
            val stopped = runBlocking { withTimeoutOrNull(APP_WORK_STOP_MS) { container.appScope.coroutineContext.job.cancelAndJoin() } }
            if (stopped == null) System.err.println("TestApp: the app's work did not stop in $APP_WORK_STOP_MS ms")
            runMainLooperDown()
            container.db.close()
        } finally {
            try {
                runBlocking { container.settings.reset() }
                FakeApps.reset()
                FakeWatchNext.reset()
                FakeRecents.reset()
                FakeChannels.reset()
            } finally {
                super.onTerminate()
            }
        }
    }

    /**
     * Runs what's posted to the main thread, frame by frame, until nothing is due within a frame (see MyPlexPlayer's
     * TestApp: Compose's main-thread dispatcher never posts again if Robolectric drops its posted run).
     */
    private fun runMainLooperDown() {
        val looper = ShadowLooper.shadowMainLooper()
        fun dueSoon(): Boolean {
            val next = looper.nextScheduledTaskTime.toMillis()
            return !looper.isIdle || (next != 0L && next <= SystemClock.uptimeMillis() + FRAME_MS)
        }
        var frames = 0
        while (dueSoon()) {
            check(++frames <= MAX_FRAMES) { "The main thread still had work due after $MAX_FRAMES frames" }
            looper.idleFor(FRAME_MS, TimeUnit.MILLISECONDS)
        }
    }

    private companion object {
        const val FRAME_MS = 17L
        const val MAX_FRAMES = 100
    }
}

/**
 * The installed apps a test's launcher sees. [hold] makes a look at them wait until [release]: for checking that Home
 * does not wait for the look.
 */
object FakeApps {
    val videos = InstalledApp("com.example.videos", "Videos")
    val music = InstalledApp("com.example.music", "Music")
    val browser = InstalledApp("com.example.browser", "Browser")

    /** As on a real TV: Android's own Settings app is on Home too, beside the launcher's own settings button. */
    val androidSettings = InstalledApp("com.android.tv.settings", "Settings")

    private val defaults = listOf(videos, music, browser, androidSettings)

    @Volatile var apps: List<InstalledApp> = defaults

    @Volatile private var held: CountDownLatch? = null

    fun list(): List<InstalledApp> {
        held?.await(UI_WAIT_MS, TimeUnit.MILLISECONDS)
        return apps
    }

    fun hold() {
        held = CountDownLatch(1)
    }

    fun release() {
        held?.countDown()
        held = null
    }

    fun reset() {
        release()
        apps = defaults
    }
}

/**
 * Other apps' Watch Next items, as a test's launcher reads them. [hold] makes a read wait until [release]; [reads]
 * counts the reads, and [watching] says whether the launcher follows changes.
 */
object FakeWatchNext : WatchNextSource {
    val film = WatchNextItem("com.example.videos/film", "com.example.videos", "Sintel", null, null, CardShape.POSTER, 0.5f, 3, null)
    val episode =
        WatchNextItem("com.example.music/ep", "com.example.music", "Tears of Steel", "Part 2", null, CardShape.WIDE, null, 2, null)

    @Volatile var items: List<WatchNextItem> = listOf(film, episode)

    @Volatile var allowed = true

    @Volatile var reads = 0

    @Volatile var watching = false

    @Volatile private var held: CountDownLatch? = null

    override fun allowed() = allowed

    override fun read(): List<WatchNextItem> {
        held?.await(UI_WAIT_MS, TimeUnit.MILLISECONDS)
        reads++
        return items
    }

    override fun watch(onChange: () -> Unit): Boolean {
        watching = true
        return true
    }

    override fun stopWatching() {
        watching = false
    }

    fun hold() {
        held = CountDownLatch(1)
    }

    fun release() {
        held?.countDown()
        held = null
    }

    fun reset() {
        release()
        items = listOf(film, episode)
        allowed = true
        reads = 0
        watching = false
    }
}

/** The apps' channels a test's launcher reads: Videos' "Free films" with one film. [reads] counts the reads. */
object FakeChannels : ChannelSource {
    val films = AppChannel(
        7,
        FakeApps.videos.packageName,
        "Free films",
        listOf(WatchNextItem("free/1", FakeApps.videos.packageName, "Big Buck Bunny", null, null, CardShape.POSTER, null, 1, null))
    )

    @Volatile var channels: List<AppChannel> = listOf(films)

    @Volatile var reads = 0

    override fun allowed() = FakeWatchNext.allowed

    override fun read(): List<AppChannel> {
        reads++
        return channels
    }

    override fun watch(onChange: () -> Unit) = true

    override fun stopWatching() = Unit

    fun reset() {
        channels = listOf(films)
        reads = 0
    }
}

/** The recent apps a test's launcher reads: Music, then Videos, with usage access given unless [allowed] is false. */
object FakeRecents : RecentsSource {
    @Volatile var allowed = true

    override fun allowed() = allowed

    override fun uses() = listOf(AppUse(FakeApps.videos.packageName, 1), AppUse(FakeApps.music.packageName, 2))

    fun reset() {
        allowed = true
    }
}

/** How long a test's end waits for the app's own work to stop. */
private const val APP_WORK_STOP_MS = 5_000L
