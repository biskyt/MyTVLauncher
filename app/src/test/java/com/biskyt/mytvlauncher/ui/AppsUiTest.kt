package com.biskyt.mytvlauncher.ui

import android.content.Intent
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
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeRecents
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 0.3.0: hide, show and move apps, All apps, and the recent apps on a double Home press. */
@RunWith(AndroidJUnit4::class)
class AppsUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private fun shown(description: String) = rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()

    /** Home with no Continue watching, so that the first app has focus. */
    private fun appsFirst() {
        FakeWatchNext.items = emptyList()
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Browser").assertIsFocused() }.isSuccess }
    }

    private fun press(code: Int) {
        rule.sendKey(code, KeyEvent.ACTION_DOWN)
        rule.sendKey(code, KeyEvent.ACTION_UP)
    }

    private fun homeOrder() = runBlocking { rule.activity.container.db.appPlaces().all() }.filter { !it.hidden }.map { it.label }

    @Test
    fun `an app hidden from Home stays in All apps, and shows on Home again from there`() {
        appsFirst()
        press(KeyEvent.KEYCODE_MENU)
        rule.waitForText("Hide from Home")
        rule.onNode(hasText("Hide from Home")).performClick()
        rule.waitAdvancingClock { !shown("Browser") }

        // The top bar button and the tile at the end of the apps row both open All apps.
        rule.onAllNodes(hasContentDescription("All apps")).onFirst().performClick()
        rule.waitAdvancingClock { shown("Browser") }
        // A heart on each app on Home: not on Browser.
        val hearts = { rule.onAllNodes(hasContentDescription("On Home")).fetchSemanticsNodes().size }
        rule.waitAdvancingClock { hearts() == 3 }
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Browser").assertIsFocused() }.isSuccess }
        press(KeyEvent.KEYCODE_MENU)
        rule.waitForText("Show on Home")
        rule.onNode(hasText("Show on Home")).performClick()
        rule.waitAdvancingClock { runBlocking { rule.activity.container.db.appPlaces().all() }.none { it.hidden } }
        rule.waitAdvancingClock { hearts() == 4 }
    }

    @Test
    fun `Move then Right moves the app one place, and OK saves the order`() {
        appsFirst()
        press(KeyEvent.KEYCODE_MENU)
        rule.waitForText("Move")
        rule.onNode(hasText("Move")).performClick()
        rule.waitForText("Left and Right move Browser")
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.waitAdvancingClock { homeOrder().take(2) == listOf("Music", "Browser") }
        assertEquals(listOf("Music", "Browser", "Settings", "Videos"), homeOrder())
    }

    @Test
    fun `Home pressed twice opens the recent apps, most recent first`() {
        openRecents()
    }

    @Test
    fun `Menu on a recent app opens its options, with no Move`() {
        openRecents()
        press(KeyEvent.KEYCODE_MENU)
        rule.waitForText("App info")
        assertTrue(rule.onAllNodes(hasText("Move")).fetchSemanticsNodes().isEmpty())
        rule.onNode(hasText("Hide from Home")).performClick()
        rule.waitAdvancingClock { runBlocking { rule.activity.container.db.appPlaces().all() }.any { it.label == "Music" && it.hidden } }
    }

    @Test
    fun `the tile under the recent apps opens All apps`() {
        openRecents()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("All apps").assertIsFocused() }.isSuccess }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        // All apps in place of Recent apps: its grid has every app, and Browser is not a recent app.
        rule.waitAdvancingClock { rule.onAllNodes(hasText("Recent apps")).fetchSemanticsNodes().isEmpty() && shown("Browser") }
    }

    @Test
    fun `without usage access, the recent apps say how to give it`() {
        FakeRecents.allowed = false
        rule.waitAdvancingClock { shown(FakeApps.videos.label) }
        pressHome()
        pressHome()
        rule.waitForText("Allow usage access")
        rule.waitForText("appops set")
    }

    /** Home pressed twice: the recent apps, with focus on the most recent, Music. */
    private fun openRecents() {
        rule.waitAdvancingClock { shown(FakeApps.videos.label) }
        pressHome()
        pressHome()
        rule.waitForText("Recent apps")
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Music").assertIsFocused() }.isSuccess }
    }

    private fun pressHome() = rule.runOnUiThread {
        rule.activity.takeIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }
}
