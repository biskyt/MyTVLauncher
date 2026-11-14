package com.biskyt.mytvlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.data.db.AppPlaceRow
import com.biskyt.mytvlauncher.data.db.WatchNextRow
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Home's first frame comes from the saved list: a slow look at the installed apps must not hold it up. */
@RunWith(AndroidJUnit4::class)
class HomeStartUiTest {
    @get:Rule val rule = createUiComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<LauncherApp>()

    @After
    fun release() {
        FakeApps.release()
        FakeWatchNext.release()
    }

    @Test
    fun `Home shows the saved apps and items, and gives focus, while the reads of the apps and Watch Next are held`() {
        runBlocking {
            // Any read started with the app ends first, so that the saved lists below are the ones shown.
            app.container.apps.look()
            app.container.watchNext.read()
            app.container.db.appPlaces().replace(listOf(AppPlaceRow("com.example.saved", "Saved app", 0)))
            app.container.db.watchNext().replace(
                listOf(WatchNextRow("k", "com.example.saved", "Saved item", null, null, CardShape.POSTER, null, 1, null))
            )
        }
        FakeApps.hold()
        FakeWatchNext.hold()
        app.container.apps.refresh()
        app.container.watchNext.refresh()
        rule.setContent { AppRoot(Navigator()) }
        rule.waitAdvancingClock { rule.onAllNodes(hasContentDescription("Saved app")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Saved item").assertIsFocused() }.isSuccess }
    }

    @Test
    fun `Continue watching read after Home shows takes the first focus, before any key is pressed`() {
        runBlocking {
            app.container.apps.look()
            app.container.watchNext.read()
            app.container.db.watchNext().deleteAll()
        }
        // Home shows with the apps only: the first app has focus.
        FakeWatchNext.hold()
        app.container.watchNext.refresh()
        rule.setContent { AppRoot(Navigator()) }
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Browser").assertIsFocused() }.isSuccess }
        // The row comes in above the apps: its first card takes the focus.
        FakeWatchNext.release()
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription(FakeWatchNext.film.title).assertIsFocused() }.isSuccess }
    }
}
