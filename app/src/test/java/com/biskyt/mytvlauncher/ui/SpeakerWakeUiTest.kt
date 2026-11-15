package com.biskyt.mytvlauncher.ui

import android.view.KeyEvent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeSpeaker
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 0.6.0: Settings › Speaker wake turns it on, steps the delay and plays the sound now. */
@RunWith(AndroidJUnit4::class)
class SpeakerWakeUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    @Test
    fun `the page turns speaker wake on, changes the delay and plays the sound`() {
        val settings = rule.activity.container.settings
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("Speaker wake")
        rule.onNode(hasText("Speaker wake")).performClick()
        rule.waitForText("Wake the speaker")
        rule.onNode(hasText("Wake the speaker")).performClick()
        rule.waitAdvancingClock { runBlocking { settings.speakerWake.first() } }
        // OK steps the delay on, and Left steps it back, as MyTVPlayer's language setting.
        rule.onNode(hasText("Delay")).performClick()
        rule.waitForText("◀ 3 s ▶")
        rule.onNode(hasText("Delay")).requestFocus()
        rule.sendKey(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_DOWN)
        rule.sendKey(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_UP)
        rule.waitForText("◀ 2 s ▶")
        rule.onNode(hasText("Play it now")).performClick()
        rule.waitAdvancingClock { FakeSpeaker.plays == 1 }
    }
}
