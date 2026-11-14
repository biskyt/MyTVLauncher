package com.biskyt.mytvlauncher.testutil

import android.os.SystemClock
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.data.InstalledApps
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

    @Volatile var apps: List<InstalledApp> = listOf(videos, music, browser)

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
        apps = listOf(videos, music, browser)
    }
}

/** How long a test's end waits for the app's own work to stop. */
private const val APP_WORK_STOP_MS = 5_000L
