package com.biskyt.mytvlauncher.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import com.biskyt.mytvlauncher.testutil.waitWithoutFrames
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccentUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    @Test
    fun `mint is the default, and a chosen accent colour is saved`() {
        val container = rule.activity.container
        waitWithoutFrames { container.accent.value != null }
        assertEquals(AccentPalette.DEFAULT, container.accent.value)

        rule.waitAdvancingClock { rule.onAllNodes(hasText("Home")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForText("Accent colour")
        rule.onNode(hasText("Accent colour")).performClick()
        rule.waitForText("Yellow")
        rule.onNode(hasText("Yellow")).performClick()
        val yellow = AccentPalette.presets.single { it.id == "yellow" }.argb
        waitWithoutFrames { container.accent.value == yellow }
    }
}
