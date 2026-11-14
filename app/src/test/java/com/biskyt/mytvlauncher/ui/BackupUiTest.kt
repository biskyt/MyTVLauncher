package com.biskyt.mytvlauncher.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.testutil.waitForText
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 0.5.0: a backup file opened with the launcher from a file manager opens Backup & restore, which restores it. */
@RunWith(AndroidJUnit4::class)
class BackupUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    @Test
    fun `a backup opened with the launcher asks first, then restores it`() {
        val container = rule.activity.container
        runBlocking { container.settings.setAccent(0xFFE5A00D.toInt()) }
        val file = File(rule.activity.cacheDir, "mytvlauncher-backup.json").apply { writeText(runBlocking { container.backup.export() }) }
        runBlocking { container.settings.setAccent(0xFFA78BFA.toInt()) }

        rule.runOnUiThread { rule.activity.takeIntent(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "application/json")) }
        rule.waitForText("This replaces your Home and choices with the backup's. Restore?")
        rule.onNode(hasText("Restore")).performClick()
        rule.waitForText("Restored.")
        rule.waitAdvancingClock { container.accent.value == 0xFFE5A00D.toInt() }
        assertEquals(null, container.openedFile.value)
    }
}
