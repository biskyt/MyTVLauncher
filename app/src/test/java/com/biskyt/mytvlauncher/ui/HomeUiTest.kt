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
import com.biskyt.mytvlauncher.data.HomeRowsNudge
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeChannels
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
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class HomeUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `with no Continue watching items, Home shows the installed apps in A to Z order, and the first one has focus`() {
        // Only the apps row: no Continue watching, and no channel row (it shows its app's name too).
        FakeWatchNext.items = emptyList()
        FakeChannels.channels = emptyList()
        runBlocking {
            rule.activity.container.watchNext.read()
            rule.activity.container.channels.read()
        }
        rule.waitAdvancingClock { shown(FakeApps.videos.label) }
        val names = FakeApps.apps.map { it.label }
        val labels = rule.onAllNodes(names.map { hasText(it) }.reduce { a, b -> a or b })
            // In their order on screen: the tree has the app with focus last, as it draws over the others.
            .fetchSemanticsNodes().sortedBy { it.boundsInRoot.left }
            .map { it.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString() }
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

    /** The remote's Back: the activity passes it to its Back handlers (BackHandler in AppRoot). */
    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.idleFrames()
    }

    private fun pressHome() = rule.runOnUiThread {
        rule.activity.takeIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    @Test
    fun `Home coming to the front asks MyTVPlayer to bring its rows up to date`() {
        rule.waitAdvancingClock {
            shadowOf(rule.activity.application).broadcastIntents.any {
                it.action == HomeRowsNudge.action("com.biskyt.mytvplayer") && it.`package` == "com.biskyt.mytvplayer"
            }
        }
    }
}
