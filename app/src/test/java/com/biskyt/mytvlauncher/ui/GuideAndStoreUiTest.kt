package com.biskyt.mytvlauncher.ui

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.data.GuideApp
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.idleFrames
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** 0.7.0: the TV guide button and its setting, the remote's Guide key, and Open in Play Store. */
@RunWith(AndroidJUnit4::class)
class GuideAndStoreUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    /** The remote's Guide key, as the TV sends it: to the activity, which handles it before the screen does. */
    private fun pressGuide() = rule.runOnUiThread {
        rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GUIDE))
        rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_GUIDE))
    }

    /** The activities the launcher started since the last call, newest last. */
    private fun started(): List<Intent> = generateSequence { shadowOf(rule.activity).nextStartedActivity }.toList()

    @Test
    fun `Settings chooses MyTVPlayer's guide, and the guide button opens it`() {
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("◀ System ▶")
        rule.onNode(hasText("TV guide")).performClick()
        rule.waitForText("◀ MyTVPlayer ▶")
        rule.waitAdvancingClock { rule.activity.container.guideApp.value == GuideApp.MY_TV_PLAYER }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.idleFrames()
        started()
        rule.onNodeWithContentDescription("TV guide").performClick()
        val guide = started().last()
        assertEquals("com.biskyt.mytvplayer.action.TV_GUIDE", guide.action)
        assertEquals("com.biskyt.mytvplayer", guide.`package`)
    }

    @Test
    fun `the remote's Guide key opens the app's guide chosen, and is left to the TV by default`() {
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription(FakeWatchNext.film.title).assertIsFocused() }.isSuccess }
        started()
        // The TV's own guide (the default): the key goes on to the TV, and the launcher opens nothing.
        pressGuide()
        assertEquals(emptyList<Intent>(), started())
        runBlocking { rule.activity.container.settings.setGuideApp(GuideApp.MY_PLEX_PLAYER) }
        rule.waitAdvancingClock { rule.activity.container.guideApp.value == GuideApp.MY_PLEX_PLAYER }
        pressGuide()
        assertEquals("com.biskyt.myplexplayer.action.TV_GUIDE", started().single().action)
    }

    @Test
    fun `Open in Play Store opens the app's page in the Play Store`() {
        FakeWatchNext.items = emptyList()
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Browser").assertIsFocused() }.isSuccess }
        rule.sendKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN)
        rule.sendKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP)
        rule.waitForText("Open in Play Store")
        started()
        rule.onNode(hasText("Open in Play Store")).performClick()
        val store = started().single()
        assertEquals("market://details?id=com.example.browser", store.dataString)
        assertEquals("com.android.vending", store.`package`)
    }
}
