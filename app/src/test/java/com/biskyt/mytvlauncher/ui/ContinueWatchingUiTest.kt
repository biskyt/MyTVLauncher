package com.biskyt.mytvlauncher.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.topRoot
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContinueWatchingUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    private fun width(description: String) = rule.onNodeWithContentDescription(description).fetchSemanticsNode().size.width

    @Test
    fun `the row shows the items newest first, as poster and wide cards, and the first has focus`() {
        rule.waitAdvancingClock { shown(FakeWatchNext.film.title) && shown(FakeWatchNext.episode.title) }
        rule.waitForText("Continue watching")
        // A poster is 124 wide, a wide card 331: the wide card is more than twice as wide.
        assertTrue(width(FakeWatchNext.episode.title) > 2 * width(FakeWatchNext.film.title))
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription(FakeWatchNext.film.title).assertIsFocused() }.isSuccess }
    }

    @Test
    fun `Menu on an item opens its options, and Hide this item takes it off the row`() {
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription(FakeWatchNext.film.title).assertIsFocused() }.isSuccess }
        rule.sendKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN)
        rule.sendKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP)
        rule.waitForText("Hide this item")
        rule.onNode(hasText("Hide this item")).performClick()
        rule.waitAdvancingClock { !shown(FakeWatchNext.film.title) }
        assertTrue(shown(FakeWatchNext.episode.title))
    }

    @Test
    fun `with the row off in Settings, Home has no row and nothing is read or watched`() {
        val container = rule.activity.container
        rule.waitAdvancingClock { shown(FakeWatchNext.film.title) }
        runBlocking { container.settings.setWatchNextShown(false) }
        rule.waitAdvancingClock { !shown(FakeWatchNext.film.title) }
        assertFalse(FakeWatchNext.watching)
        val reads = FakeWatchNext.reads
        container.watchNext.refresh()
        runBlocking { container.watchNext.read() }
        assertEquals(reads, FakeWatchNext.reads)
        assertTrue(runBlocking { container.db.watchNext().all() }.isEmpty())
    }

    @Test
    fun `an app turned off in Home rows takes its items off the row`() {
        val container = rule.activity.container
        rule.waitAdvancingClock { shown(FakeWatchNext.episode.title) }
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("Home rows")
        rule.onNode(hasText("Home rows")).performClick()
        rule.waitForText("Music")
        rule.onNode(hasText("Music")).performClick()
        rule.waitAdvancingClock { runBlocking { container.watchNextItems.value?.map { it.title } } == listOf(FakeWatchNext.film.title) }
    }

    @Test
    fun `without the permission, the row asks for it`() {
        FakeWatchNext.allowed = false
        rule.activityRule.scenario.recreate()
        rule.waitForText("Allow Continue watching")
        assertFalse(shown(FakeWatchNext.film.title))
        rule.topRoot()
    }
}
