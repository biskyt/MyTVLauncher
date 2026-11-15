package com.biskyt.mytvlauncher.data

import android.database.MatrixCursor
import android.media.tv.TvContract.PreviewPrograms
import android.media.tv.TvContract.WatchNextPrograms
import android.provider.BaseColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** A Watch Next row from the TV provider, read as core's item. */
@RunWith(AndroidJUnit4::class)
class WatchNextCursorTest {
    private val columns = arrayOf(
        BaseColumns._ID,
        WatchNextPrograms.COLUMN_PACKAGE_NAME,
        WatchNextPrograms.COLUMN_TITLE,
        WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
        WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
        WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
        WatchNextPrograms.COLUMN_DURATION_MILLIS,
        WatchNextPrograms.COLUMN_TYPE
    )

    private fun read(vararg row: Any?) = MatrixCursor(columns).apply { addRow(row) }.run {
        moveToFirst()
        toItem()!!
    }

    @Test
    fun `the season, episode and time left come from the row`() {
        val item = read(1L, "com.example.videos", "Show", "1", "2", 15 * 60_000L, 45 * 60_000L, WatchNextPrograms.TYPE_TV_EPISODE)
        assertEquals("1", item.season)
        assertEquals("2", item.episode)
        assertEquals(30 * 60_000L, item.leftMs)
    }

    @Test
    fun `a TV channel's card is a logo card`() {
        val item = read(1L, "com.example.tv", "BBC One", null, null, null, null, PreviewPrograms.TYPE_CHANNEL)
        assertEquals(CardShape.LOGO, item.shape)
    }

    @Test
    fun `blank numbers and no length give nothing`() {
        val item = read(1L, "com.example.videos", "Film", " ", null, null, null, null)
        assertNull(item.season)
        assertNull(item.episode)
        assertNull(item.leftMs)
    }
}
