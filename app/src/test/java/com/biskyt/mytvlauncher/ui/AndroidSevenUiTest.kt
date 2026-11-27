package com.biskyt.mytvlauncher.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeChannels
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import com.biskyt.mytvlauncher.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Android 7.1 (Fire OS 6) has no Watch Next or app channels: Home shows the apps only, and asks for nothing. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [25])
class AndroidSevenUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(text: String) = rule.onAllNodes(hasText(text) or hasContentDescription(text)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `Home shows the apps with no Continue watching, channels or question, and reads nothing`() {
        rule.waitAdvancingClock { shown(FakeApps.videos.label) }
        val c = rule.activity.container
        rule.waitAdvancingClock(stepMs = 100) { c.watchNextShown.value == false && c.channelsShown.value == false }
        assertFalse(shown("Continue watching"))
        assertFalse(shown(FakeWatchNext.film.title))
        assertFalse(rule.onAllNodes(hasText("Allow", substring = true)).fetchSemanticsNodes().isNotEmpty())
        assertEquals(0, FakeWatchNext.reads)
        assertEquals(0, FakeChannels.reads)
    }

    @Test
    fun `Settings has no Home rows, as there is nothing to set`() {
        rule.waitAdvancingClock { shown(FakeApps.videos.label) }
        rule.runOnUiThread { rule.activity.nav.open(Screen.SETTINGS) }
        rule.waitForText("Accent colour")
        assertFalse(shown("Home rows"))
    }
}
