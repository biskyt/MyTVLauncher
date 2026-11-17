package com.biskyt.mytvlauncher.ui

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.keys.HomePressLog
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.focusedText
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.settle
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import com.biskyt.mytvlauncher.testutil.waitOnHome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Home pressed once on Home shows Home's start again (the owner, 2026-11-17). */
@RunWith(AndroidJUnit4::class)
class HomeResetUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private val films = (0 until 12).map {
        WatchNextItem("film/$it", FakeApps.videos.packageName, "Film $it", null, null, CardShape.POSTER, null, 100L - it, null)
    }

    private fun focused(description: String) = runCatching { rule.onNodeWithContentDescription(description).assertIsFocused() }.isSuccess

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    private fun press(code: Int) {
        rule.sendKey(code, KeyEvent.ACTION_DOWN)
        rule.sendKey(code, KeyEvent.ACTION_UP)
        rule.waitAdvancingClock(stepMs = 20) { true }
    }

    private fun pressHome() = rule.runOnUiThread {
        rule.activity.takeIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    @Test
    fun `Home pressed on Home scrolls every row back and gives the first app focus`() {
        FakeWatchNext.items = films
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitOnHome(rule.activity.container, "Continue watching's first card has no focus") { focused("Film 0") }
        // Along Continue watching until its first card is out of view, then down to the apps row, past its first app.
        repeat(10) { press(KeyEvent.KEYCODE_DPAD_RIGHT) }
        rule.waitAdvancingClock { focused("Film 10") }
        assertFalse("Film 0 is still in view", shown("Film 0"))
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.waitAdvancingClock { rule.focusedText().contains("All apps") }

        pressHome()
        rule.waitAdvancingClock(stepMs = 50) { focused("Browser") }
        rule.onNodeWithContentDescription("Film 0").assertIsDisplayed()
        assertTrue(rule.activity.nav.homeReset > 0)
    }

    @Test
    fun `a double press opens the recent apps and does not reset Home`() {
        rule.waitOnHome(rule.activity.container, "Continue watching's first card has no focus") { focused(FakeWatchNext.film.title) }
        pressHome()
        pressHome()
        rule.waitForText("Recent apps")
        // Long enough for a reset to come, had the first press made one.
        rule.waitAdvancingClock(stepMs = HomePressLog.DOUBLE_PRESS_MS + 100) { true }
        rule.settle()
        assertEquals(0L, rule.activity.nav.homeReset)
        assertEquals(Screen.RECENTS, rule.activity.nav.top)
    }

    @Test
    fun `Home pressed on another screen goes back to Home and does not reset it`() {
        rule.waitOnHome(rule.activity.container, "Continue watching's first card has no focus") { focused(FakeWatchNext.film.title) }
        rule.runOnUiThread { rule.activity.nav.open(Screen.SETTINGS) }
        rule.waitForText("Accent colour")
        pressHome()
        rule.waitAdvancingClock(stepMs = HomePressLog.DOUBLE_PRESS_MS + 100) { true }
        rule.settle()
        assertEquals(Screen.HOME, rule.activity.nav.top)
        assertEquals(0L, rule.activity.nav.homeReset)
    }
}
