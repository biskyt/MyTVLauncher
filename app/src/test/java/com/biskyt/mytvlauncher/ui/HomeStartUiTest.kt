package com.biskyt.mytvlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.data.db.AppPlaceRow
import com.biskyt.mytvlauncher.testutil.FakeApps
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
    fun releaseApps() = FakeApps.release()

    @Test
    fun `Home shows the saved apps and gives one focus while the look at the installed apps is held`() {
        runBlocking { app.container.db.appPlaces().replace(listOf(AppPlaceRow("com.example.saved", "Saved app", 0))) }
        FakeApps.hold()
        app.container.apps.refresh()
        rule.setContent { AppRoot(Navigator()) }
        rule.waitAdvancingClock { rule.onAllNodes(hasContentDescription("Saved app")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitAdvancingClock { runCatching { rule.onNodeWithContentDescription("Saved app").assertIsFocused() }.isSuccess }
    }
}
