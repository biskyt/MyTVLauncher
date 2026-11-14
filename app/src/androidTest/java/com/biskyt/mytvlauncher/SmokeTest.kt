package com.biskyt.mytvlauncher

import android.content.ContentUris
import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The launcher on a real Android TV: what Robolectric cannot show. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theLauncherIsAHomeApp() {
        val homes = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        assertTrue(homes.any { it.activityInfo.packageName == context.packageName })
    }

    @Test
    fun homeShowsTheTvsAppsAndOpensSettings() {
        // The emulator's own apps (its settings, at least) are on Home: the manifest's <queries> lets the launcher see them.
        rule.waitUntil(WAIT_MS) { runBlocking { context.container.db.appPlaces().all() }.isNotEmpty() }
        rule.waitUntilAtLeastOneExists(hasContentDescription("Launcher settings"), WAIT_MS)
        rule.onNode(hasContentDescription("Launcher settings")).performClick()
        rule.waitUntilAtLeastOneExists(hasText("Watch Next check"), WAIT_MS)
    }

    @Test
    fun theWatchNextCheckCountsTheLaunchersOwnItem() {
        val program = WatchNextProgram.Builder()
            .setType(TvContractCompat.WatchNextPrograms.TYPE_MOVIE)
            .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
            .setLastEngagementTimeUtcMillis(System.currentTimeMillis())
            .setTitle("Smoke test")
            .build()
        val uri = context.contentResolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, program.toContentValues())
        try {
            val count = runBlocking { context.container.listings.check() }.watchNext.count ?: 0
            assertTrue("Watch Next items read: $count", count >= 1)
        } finally {
            uri?.let { context.contentResolver.delete(TvContractCompat.buildWatchNextProgramUri(ContentUris.parseId(it)), null, null) }
        }
    }

    private companion object {
        const val WAIT_MS = 20_000L
    }
}
