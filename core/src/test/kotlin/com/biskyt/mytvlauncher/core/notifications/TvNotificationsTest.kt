package com.biskyt.mytvlauncher.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class TvNotificationsTest {
    @Test
    fun `notifications show newest first, without the launcher's own, group summaries or empty ones`() {
        val all = listOf(
            TvNotification("a", "com.example.music", "Now playing", null, postedAt = 1),
            TvNotification("b", "com.example.videos", "Download done", "Big Buck Bunny", postedAt = 3),
            TvNotification("c", "com.biskyt.mytvlauncher", "Own", null, postedAt = 4),
            TvNotification("d", "com.example.videos", "2 downloads", null, postedAt = 5, summary = true),
            TvNotification("e", "com.example.store", " ", null, postedAt = 6),
            TvNotification("f", "com.example.music", "Player", "Playing", postedAt = 7, clearable = false)
        )
        assertEquals(listOf("b", "a"), TvNotifications.shown(all, own = "com.biskyt.mytvlauncher").map { it.key })
    }
}
