package com.biskyt.mytvlauncher.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeChannels
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 0.4.0: the apps' channel rows, their switches, and the top bar's clock and network status. */
@RunWith(AndroidJUnit4::class)
class ChannelsUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private val film = FakeChannels.films.items.single().title

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `a channel's row shows its items under its name and its app's`() {
        rule.waitForText("Free films")
        rule.waitAdvancingClock { shown(film) }
    }

    @Test
    fun `a channel turned off in Home rows leaves Home`() {
        rule.waitAdvancingClock { shown(film) }
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.onNode(hasText("Home rows")).performClick()
        rule.waitForText("Free films")
        rule.onNode(hasText("Free films")).performClick()
        rule.waitAdvancingClock { rule.activity.container.channelRows.value.isNullOrEmpty() }
    }

    @Test
    fun `with the channel rows off, nothing is read and the saved rows go`() {
        val container = rule.activity.container
        rule.waitAdvancingClock { shown(film) }
        runBlocking { container.settings.setChannelsShown(false) }
        rule.waitAdvancingClock { !shown(film) }
        val reads = FakeChannels.reads
        container.channels.refresh()
        runBlocking { container.channels.read() }
        assertEquals(reads, FakeChannels.reads)
        assertTrue(runBlocking { container.db.channelItems().all() }.isEmpty())
    }

    @Test
    fun `the top bar shows the network status`() {
        rule.waitAdvancingClock { shown("No network") || shown("Wi-Fi") || shown("Ethernet") || shown("Connected") }
    }
}
