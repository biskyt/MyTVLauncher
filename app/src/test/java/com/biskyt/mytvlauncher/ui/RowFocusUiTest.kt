package com.biskyt.mytvlauncher.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitOnHome
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Left and Right never leave a row (the owner, 2026-11-15). */
@RunWith(AndroidJUnit4::class)
class RowFocusUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun focused(description: String) = runCatching { rule.onNodeWithContentDescription(description).assertIsFocused() }.isSuccess

    private fun press(code: Int) {
        rule.sendKey(code, KeyEvent.ACTION_DOWN)
        rule.sendKey(code, KeyEvent.ACTION_UP)
        rule.waitAdvancingClock(stepMs = 20) { true }
        repeat(5) { rule.waitAdvancingClock { true } }
    }

    @Test
    fun `Right off a row's last card and Left off its first stay on the card`() {
        rule.waitOnHome(rule.activity.container, "Continue watching's first card has no focus") { focused(FakeWatchNext.film.title) }
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        assertTrue("Left left the row", focused(FakeWatchNext.film.title))
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.waitAdvancingClock { focused(FakeWatchNext.episode.title) }
        // The row's last card: Right went up to the top bar before.
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue("Right left the row", focused(FakeWatchNext.episode.title))
        // Down still leaves it, for the apps row.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.waitAdvancingClock { !focused(FakeWatchNext.episode.title) }
    }
}
