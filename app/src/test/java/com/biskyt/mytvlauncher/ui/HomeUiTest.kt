package com.biskyt.mytvlauncher.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.idleFrames
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `with no Continue watching items, Home shows the installed apps in A to Z order, and the first one has focus`() {
        FakeWatchNext.items = emptyList()
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitAdvancingClock { shown(FakeApps.videos.label) }
        val names = FakeApps.apps.map { it.label }
        val labels = rule.onAllNodes(names.map { hasText(it) }.reduce { a, b -> a or b })
            .fetchSemanticsNodes().map { it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString() }
        assertEquals(listOf("Browser", "Music", "Settings", "Videos"), labels)
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Browser").assertIsFocused() }.isSuccess }
    }

    @Test
    fun `Back on Home stays on Home, and Back from Settings goes back to it`() {
        rule.waitAdvancingClock { shown("Browser") }
        pressBack()
        assertTrue(shown("Browser"))

        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("Accent colour")
        pressBack()
        rule.waitAdvancingClock { shown("Browser") }
    }

    @Test
    fun `Home pressed goes back to Home, and is logged`() {
        rule.waitAdvancingClock { shown("Browser") }
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("Accent colour")
        pressHome()
        rule.waitAdvancingClock { shown("Browser") }
        assertEquals(1, rule.activity.container.homePresses.value.size)
    }

    @Test
    fun `the Home key test stays open when Home is pressed, and lists each press`() {
        rule.waitAdvancingClock { shown("Browser") }
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("Watch Next check")
        rule.onNode(hasText("Watch Next check")).performClick()
        rule.waitForText("No Home signals yet.")
        pressHome()
        pressHome()
        rule.waitForText("ms after the last one")
        rule.waitForText("Home key test")
    }

    /** The remote's Back: the activity passes it to its Back handlers (BackHandler in AppRoot). */
    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.idleFrames()
    }

    private fun pressHome() = rule.runOnUiThread {
        rule.activity.takeIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }
}
