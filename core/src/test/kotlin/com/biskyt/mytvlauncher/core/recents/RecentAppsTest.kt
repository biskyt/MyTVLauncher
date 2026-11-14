package com.biskyt.mytvlauncher.core.recents

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsTest {
    @Test
    fun `most recent first, each once, only apps the launcher shows`() {
        val uses = listOf(
            AppUse("videos", 10),
            AppUse("music", 30),
            AppUse("videos", 40),
            AppUse("launcher", 50),
            AppUse("service", 60),
            AppUse("news", 0)
        )
        val order = RecentApps.order(uses, setOf("videos", "music", "news"))
        assertEquals(listOf("videos", "music"), order)
    }

    @Test
    fun `at most the limit`() {
        val uses = (1L..20L).map { AppUse("app$it", it) }
        assertEquals(12, RecentApps.order(uses, uses.map { it.packageName }.toSet()).size)
    }
}
