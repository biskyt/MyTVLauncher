package com.biskyt.mytvlauncher.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeChannels
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The owner's points on 0.4.0 (2026-11-14): channels, card size, Back on a row, and moving apps in All apps. */
@RunWith(AndroidJUnit4::class)
class HomeFixesUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    private fun focused(description: String) = runCatching { rule.onNodeWithContentDescription(description).assertIsFocused() }.isSuccess

    private fun press(code: Int) {
        rule.sendKey(code, KeyEvent.ACTION_DOWN)
        rule.sendKey(code, KeyEvent.ACTION_UP)
    }

    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun width(description: String) = rule.onNodeWithContentDescription(description).fetchSemanticsNode().size.width

    @Test
    fun `a channel the TV does not show is listed under its app, and shows on Home once turned on`() {
        val extra = FakeChannels.extra.items.single().title
        rule.waitAdvancingClock { shown(FakeChannels.films.items.single().title) }
        assertFalse(shown(extra))
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.onNode(hasText("Home rows")).performClick()
        rule.waitForText("More films")
        rule.waitForText("Music")
        rule.onNode(hasText("More films")).performClick()
        rule.waitAdvancingClock { rule.activity.container.channelRows.value.orEmpty().any { it.id == FakeChannels.extra.id } }
    }

    @Test
    fun `channel cards are half the size of Continue watching's`() {
        val film = FakeChannels.films.items.single().title
        rule.waitAdvancingClock { shown(film) && shown(FakeWatchNext.film.title) }
        // Both are posters: 124 wide in Continue watching, 62 in a channel row.
        assertTrue(abs(width(FakeWatchNext.film.title) - 2 * width(film)) <= 2)
    }

    @Test
    fun `Back on a row's later card goes back to the row's first card`() {
        rule.waitAdvancingClock { focused(FakeWatchNext.film.title) }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.waitAdvancingClock { focused(FakeWatchNext.episode.title) }
        back()
        rule.waitAdvancingClock { focused(FakeWatchNext.film.title) }

        // The apps row too.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.waitAdvancingClock { focused("Browser") }
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        rule.waitAdvancingClock { focused("Settings") }
        back()
        rule.waitAdvancingClock { focused("Browser") }
    }

    @Test
    fun `Move in All apps moves the app, and OK saves the order for Home too`() {
        rule.waitAdvancingClock { shown(FakeWatchNext.film.title) }
        rule.onAllNodes(hasContentDescription("All apps")).onFirst().performClick()
        rule.waitAdvancingClock { focused("Browser") }
        press(KeyEvent.KEYCODE_MENU)
        rule.waitForText("Move")
        rule.onNode(hasText("Move")).performClick()
        rule.waitForText("The arrows move Browser. OK or Back ends.")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        val order = { runBlocking { rule.activity.container.db.appPlaces().all() }.map { it.label } }
        rule.waitAdvancingClock { order().take(2) == listOf("Music", "Browser") }
        assertEquals(listOf("Music", "Browser", "Settings", "Videos"), order())
    }

    @Test
    fun `Row order moves the apps above Continue watching, and Home follows it`() {
        val container = rule.activity.container
        rule.waitAdvancingClock { focused(FakeWatchNext.film.title) }
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.onNode(hasText("Home rows")).performClick()
        rule.waitForText("Row order")
        rule.onNode(hasText("Row order")).performClick()
        rule.waitForText("OK on a row picks it up. Up and Down move it, and OK or Back puts it down.")
        rule.waitAdvancingClock { runCatching { rule.onNode(hasText("Continue watching")).assertIsFocused() }.isSuccess }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitForText("Up and Down move Apps. OK or Back ends.")
        press(KeyEvent.KEYCODE_DPAD_UP)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitAdvancingClock { container.rowOrder.value.orEmpty().take(2) == listOf("apps", "watch_next") }
        repeat(3) { back() }
        rule.waitAdvancingClock { shown("Browser") && shown(FakeWatchNext.film.title) }
        val top = { d: String -> rule.onNodeWithContentDescription(d).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(top("Browser") < top(FakeWatchNext.film.title))
    }
}
