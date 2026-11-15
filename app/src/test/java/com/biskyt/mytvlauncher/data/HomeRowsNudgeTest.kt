package com.biskyt.mytvlauncher.data

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The launcher asks MyTVPlayer to bring its rows on Home up to date (the owner, 2026-11-15). */
@RunWith(AndroidJUnit4::class)
class HomeRowsNudgeTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private var time = 0L

    private fun nudges() = shadowOf(app).broadcastIntents.filter { it.action == HomeRowsNudge.action("com.biskyt.mytvplayer") }

    private fun nudge(rowsShown: Boolean = true) =
        HomeRowsNudge(app, flowOf(rowsShown), CoroutineScope(Dispatchers.Unconfined), waitMs = 0) {
            time
        }

    @Test
    fun `the request goes to MyTVPlayer only, once in five minutes`() {
        shadowOf(app).clearBroadcastIntents()
        val n = nudge()
        n.nudge()
        assertEquals(listOf("com.biskyt.mytvplayer"), nudges().map { it.`package` })
        // Also to MyTVPlayer in the stopped state.
        assertTrue(nudges().all { it.flags and Intent.FLAG_INCLUDE_STOPPED_PACKAGES != 0 })
        time += HomeRowsNudge.MIN_GAP_MS - 1
        n.nudge()
        assertEquals(1, nudges().size)
        time += 1
        n.nudge()
        assertEquals(2, nudges().size)
    }

    @Test
    fun `with Continue watching and the channel rows off, nothing is asked`() {
        shadowOf(app).clearBroadcastIntents()
        nudge(rowsShown = false).nudge()
        assertEquals(0, nudges().size)
    }
}
