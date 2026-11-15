package com.biskyt.mytvlauncher.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.core.rows.RowOrder
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import com.biskyt.mytvlauncher.data.db.HiddenKind
import com.biskyt.mytvlauncher.data.db.HiddenRow
import com.biskyt.mytvlauncher.testutil.FakeApps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** 0.5.0: a backup holds the settings, the apps' order and hidden apps, and the user's choices, and restores them. */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private val container = ApplicationProvider.getApplicationContext<LauncherApp>().container

    private fun shownApps() = runBlocking { container.apps.all.first() }.filter { !it.hidden }.map { it.label }

    @Test
    fun `a restore brings back the settings, the apps' order and hidden apps, and the choices`() = runBlocking {
        container.apps.look()
        container.apps.saveOrder(listOf(FakeApps.videos.packageName, FakeApps.music.packageName))
        container.apps.setHidden(FakeApps.browser.packageName, true)
        container.settings.setAccent(0xFFE5A00D.toInt())
        container.settings.setChannelsShown(false)
        container.settings.setRowOrder(listOf(RowOrder.APPS, RowOrder.WATCH_NEXT))
        container.watchNext.hideItem("k1")
        container.channels.setChannelShown(8, true)
        container.settings.setSpeakerWake(true)
        container.settings.setSpeakerWakeDelay(5)
        val saved = container.backup.export()

        // Everything changed after the backup.
        container.settings.reset()
        container.apps.setHidden(FakeApps.browser.packageName, false)
        container.apps.saveOrder(listOf(FakeApps.browser.packageName, FakeApps.androidSettings.packageName))
        container.watchNext.showAllItems()
        container.channels.setChannelShown(8, false)

        container.backup.restore(saved.byteInputStream())
        container.apps.look()
        val settings = container.settings.saved()
        assertEquals(0xFFE5A00D.toInt(), settings.accent)
        assertEquals(false, settings.channelsShown)
        assertEquals(listOf(RowOrder.APPS, RowOrder.WATCH_NEXT), settings.rowOrder)
        assertEquals(true, settings.speakerWake)
        assertEquals(5, settings.speakerWakeDelay)
        assertEquals(listOf("Videos", "Music", "Settings"), shownApps())
        assertEquals(
            setOf(HiddenRow(HiddenKind.WATCH_NEXT_ITEM, "k1"), HiddenRow(HiddenKind.CHANNEL_ON, "8")),
            container.db.hidden().all().toSet()
        )
    }

    @Test
    fun `an app in the backup that is not installed goes, and one installed here goes at the end`() = runBlocking {
        container.apps.look()
        val saved = container.backup.export()
        val radio = InstalledApp("com.example.radio", "Radio")
        FakeApps.apps = listOf(FakeApps.videos, FakeApps.browser, radio)
        container.backup.restore(saved.byteInputStream())
        container.apps.look()
        assertEquals(listOf("Browser", "Videos", "Radio"), shownApps())
    }

    @Test
    fun `a file that is not a backup, or one from a newer version, changes nothing`() = runBlocking {
        container.settings.setAccent(0xFFA78BFA.toInt())
        val other =
            assertThrows(BackupException::class.java) { runBlocking { container.backup.restore("{\"app\":\"Other\"}".byteInputStream()) } }
        assertEquals(BackupException.Reason.NOT_A_BACKUP, other.reason)
        val notJson = assertThrows(BackupException::class.java) { runBlocking { container.backup.restore("not json".byteInputStream()) } }
        assertEquals(BackupException.Reason.NOT_A_BACKUP, notJson.reason)
        val newer = container.backup.export().replace("\"version\": 1", "\"version\": 99")
        val later = assertThrows(BackupException::class.java) { runBlocking { container.backup.restore(newer.byteInputStream()) } }
        assertEquals(BackupException.Reason.NEWER, later.reason)
        assertEquals(0xFFA78BFA.toInt(), container.settings.saved().accent)
    }

    @Test
    fun `a file over 1 MB is not a backup, and a colour that is not offered restores as the default`() = runBlocking {
        val big = assertThrows(BackupException::class.java) {
            runBlocking { container.backup.restore(ByteArray((1 shl 20) + 1) { ' '.code.toByte() }.inputStream()) }
        }
        assertEquals(BackupException.Reason.NOT_A_BACKUP, big.reason)
        container.settings.setAccent(0xFF000000.toInt())
        container.backup.restore(container.backup.export().byteInputStream())
        assertEquals(AccentPalette.DEFAULT, container.settings.saved().accent)
    }
}
