package com.biskyt.mytvlauncher.data

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.testutil.FakeSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * 0.6.0: the speaker wake sound when the TV wakes, once for each wake, only with the setting on and nothing playing.
 * The wakes are [SpeakerWake.woke] calls: a broadcast in a test can reach a receiver left by an earlier test's app.
 */
@RunWith(AndroidJUnit4::class)
class SpeakerWakeTest {
    private val app = ApplicationProvider.getApplicationContext<LauncherApp>()
    private val container = app.container

    private fun wakeAndWait() = runBlocking {
        container.speakerWake.woke()
        container.speakerWake.idle()
    }

    @Test
    fun `the launcher follows the TV's wakes`() {
        val receivers = shadowOf(app).registeredReceivers.filter { it.intentFilter.hasAction(Intent.ACTION_SCREEN_ON) }
        assertTrue(receivers.any { it.broadcastReceiver.javaClass.name.startsWith(SpeakerWake::class.java.name) })
    }

    @Test
    fun `with the setting on, a wake plays the sound once`() = runBlocking {
        container.settings.setSpeakerWake(true)
        container.settings.setSpeakerWakeDelay(0)
        wakeAndWait()
        assertEquals(1, FakeSpeaker.plays)
    }

    @Test
    fun `nothing plays with the setting off, or while other sound plays`() = runBlocking {
        container.settings.setSpeakerWakeDelay(0)
        wakeAndWait()
        assertEquals(0, FakeSpeaker.plays)
        container.settings.setSpeakerWake(true)
        FakeSpeaker.playing = true
        wakeAndWait()
        assertEquals(0, FakeSpeaker.plays)
    }

    @Test
    fun `a second wake during the delay cancels the first, so the sound plays once`() = runBlocking {
        container.settings.setSpeakerWake(true)
        container.settings.setSpeakerWakeDelay(2)
        container.speakerWake.woke()
        // Well inside the first wake's 2 s delay.
        delay(200)
        wakeAndWait()
        assertEquals(1, FakeSpeaker.plays)
    }
}
