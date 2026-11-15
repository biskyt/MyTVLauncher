package com.biskyt.mytvlauncher.ui

import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeNotifications
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** 0.8.0: system notifications, from the top bar's bell, while Settings › Show notifications is on. */
@RunWith(AndroidJUnit4::class)
class NotificationsUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `with the setting off (the default), there is no bell and nothing is followed`() {
        rule.waitAdvancingClock { shown("Browser") }
        assertFalse(shown("2 notifications"))
        assertFalse(FakeNotifications.following)
    }

    @Test
    fun `turning the setting on follows the notifications and asks for access when it is missing`() {
        FakeNotifications.allowed = false
        rule.activity.container.checkNotificationAccess()
        rule.onNodeWithContentDescription("Launcher settings").performClick()
        rule.waitForText("Show notifications")
        rule.onNode(hasText("Show notifications")).performClick()
        rule.waitAdvancingClock { FakeNotifications.following }
        val asked = shadowOf(rule.activity).nextStartedActivity
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS, asked.action)
        // Until access is given, the adb command shows.
        rule.waitForText("adb shell cmd notification allow_listener")
    }

    @Test
    fun `the bell counts the notifications, and its panel opens and dismisses them`() {
        runBlocking { rule.activity.container.settings.setNotificationsShown(true) }
        rule.waitAdvancingClock { shown("2 notifications") }
        rule.onNodeWithContentDescription("2 notifications").performClick()
        rule.waitForText("Download done")
        // Newest first: the panel's first row has focus. Menu dismisses it.
        rule.sendKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN)
        rule.sendKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP)
        rule.waitAdvancingClock { FakeNotifications.all.value.none { it.key == FakeNotifications.download.key } }
        rule.onNode(hasText("Now playing")).performClick()
        rule.waitAdvancingClock { FakeNotifications.opened == FakeNotifications.playing.key }
        assertTrue(shown("1 notification"))
    }
}
