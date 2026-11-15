package com.biskyt.mytvlauncher.testutil

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.biskyt.mytvlauncher.AppContainer
import kotlinx.coroutines.runBlocking

/** What Home has from its reads: for a wait's failure message. */
fun AppContainer.homeState(): String =
    "items=${watchNextItems.value?.map { it.title }} watchOn=${watchNextShown.value} allowed=${watchNext.allowed.value} " +
        "saved=${runBlocking { db.watchNext().all() }.size} reads=${FakeWatchNext.reads} " +
        "channels=${channelRows.value?.map { it.name }} channelsOn=${channelsShown.value} channelReads=${FakeChannels.reads} " +
        "apps=${shownApps.value?.size} order=${rowOrder.value}"

/** Waits for [condition] on Home; on a timeout, says what Home had, with the screen and the other threads. */
fun ComposeTestRule.waitOnHome(container: AppContainer, what: String, condition: () -> Boolean) = try {
    waitAdvancingClock(condition = condition)
} catch (e: ComposeTimeoutException) {
    throw AssertionError("$what. ${container.homeState()}\nScreen:\n${screen()}\nThreads:\n${otherThreads()}", e)
}
