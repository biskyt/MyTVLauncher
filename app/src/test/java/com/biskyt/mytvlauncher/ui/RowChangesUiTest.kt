package com.biskyt.mytvlauncher.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.focusedText
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitOnHome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A new or changed item in a row (the owner, 2026-11-22): the row scrolls to its start, and the item takes the focus if
 * the row had it. The focus in another row stays where it is.
 */
@RunWith(AndroidJUnit4::class)
class RowChangesUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private val films = (0 until 12).map { film(it, progress = null) }

    private fun film(i: Int, progress: Float?, at: Long = 100L - i) =
        WatchNextItem("film/$i", FakeApps.videos.packageName, "Film $i", null, null, CardShape.POSTER, progress, at, null)

    private fun focused(description: String) = runCatching { rule.onNodeWithContentDescription(description).assertIsFocused() }.isSuccess

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    private fun press(code: Int) {
        rule.sendKey(code, KeyEvent.ACTION_DOWN)
        rule.sendKey(code, KeyEvent.ACTION_UP)
        rule.waitAdvancingClock(stepMs = 20) { true }
    }

    private fun show(items: List<WatchNextItem>) {
        FakeWatchNext.items = items
        runBlocking { rule.activity.container.watchNext.read() }
    }

    /** Along Continue watching until its first card is out of view. */
    @Before
    fun alongTheRow() {
        show(films)
        rule.waitOnHome(rule.activity.container, "Continue watching's first card has no focus") { focused("Film 0") }
        repeat(10) { press(KeyEvent.KEYCODE_DPAD_RIGHT) }
        rule.waitAdvancingClock { focused("Film 10") }
        assertFalse("Film 0 is still in view", shown("Film 0"))
    }

    @Test
    fun `a new item scrolls its row to the start and takes the focus from that row`() {
        show(listOf(film(99, progress = 0.1f, at = 200)) + films)
        rule.waitAdvancingClock { focused("Film 99") }
        rule.waitAdvancingClock { shown("Film 0") }
    }

    @Test
    fun `a changed item takes the focus from its row`() {
        show(films.map { if (it.title == "Film 3") it.copy(progress = 0.5f) else it })
        rule.waitAdvancingClock { focused("Film 3") }
        rule.waitAdvancingClock { shown("Film 0") }
    }

    @Test
    fun `a changed item too far along for the start of the row still takes the focus`() {
        show(films.map { if (it.title == "Film 11") it.copy(progress = 0.5f) else it })
        rule.waitAdvancingClock { focused("Film 11") }
    }

    @Test
    fun `a change in a row without the focus scrolls it to the start and leaves the focus where it is`() {
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        // Down from the far right of Continue watching: the end of the apps row.
        rule.waitAdvancingClock { rule.focusedText().contains("All apps") }
        show(listOf(film(99, progress = 0.1f, at = 200)) + films)
        rule.waitAdvancingClock(stepMs = 20) { shown("Film 99") && shown("Film 0") }
        repeat(5) { rule.waitAdvancingClock(stepMs = 20) { true } }
        assertTrue("The focus left the apps row: ${rule.focusedText()}", rule.focusedText().contains("All apps"))
    }
}
