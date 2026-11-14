package com.biskyt.mytvlauncher.core.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderTest {
    private val videos = InstalledApp("com.example.videos", "Videos")
    private val music = InstalledApp("com.example.music", "music")
    private val browser = InstalledApp("com.example.browser", "Browser")

    @Test
    fun `new apps go after the others in A to Z order`() {
        val places = AppOrder.merge(listOf(videos, music, browser), emptyList())
        assertEquals(listOf("com.example.browser", "com.example.music", "com.example.videos"), places.map { it.packageName })
        assertEquals(listOf(0, 1, 2), places.map { it.position })
    }

    @Test
    fun `saved apps keep their place and hidden state, and a new app goes at the end`() {
        val saved = listOf(AppPlace(videos.packageName, 0), AppPlace(music.packageName, 1, hidden = true))
        val places = AppOrder.merge(listOf(browser, music, videos), saved)
        assertEquals(
            listOf(AppPlace(videos.packageName, 0), AppPlace(music.packageName, 1, hidden = true), AppPlace(browser.packageName, 2)),
            places
        )
    }

    @Test
    fun `a removed app loses its place, and the others close the gap`() {
        val saved = listOf(AppPlace(videos.packageName, 0), AppPlace(music.packageName, 1), AppPlace(browser.packageName, 2))
        val places = AppOrder.merge(listOf(videos, browser), saved)
        assertEquals(listOf(AppPlace(videos.packageName, 0), AppPlace(browser.packageName, 1)), places)
    }

    @Test
    fun `Home shows only the apps that are not hidden, in their order`() {
        val places =
            listOf(AppPlace(browser.packageName, 1), AppPlace(videos.packageName, 0), AppPlace(music.packageName, 2, hidden = true))
        assertEquals(listOf(videos.packageName, browser.packageName), AppOrder.shown(places).map { it.packageName })
    }
}
