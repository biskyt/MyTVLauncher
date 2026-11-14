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
    fun `an app moves one place at a time, and stays put at the ends`() {
        val order = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), AppOrder.move(order, "a", 1))
        assertEquals(listOf("a", "c", "b"), AppOrder.move(order, "c", -1))
        assertEquals(order, AppOrder.move(order, "a", -1))
        assertEquals(order, AppOrder.move(order, "c", 1))
        // A grid row's step past the end goes to the end (Down onto a short last row in All apps).
        assertEquals(listOf("b", "c", "a"), AppOrder.move(order, "a", 4))
    }

    @Test
    fun `reordering keeps each hidden app after the app it followed`() {
        val places = listOf(AppPlace("a", 0), AppPlace("h", 1, hidden = true), AppPlace("b", 2), AppPlace("c", 3))
        val after = AppOrder.reorder(places, listOf("b", "a", "c"))
        assertEquals(listOf("b", "a", "h", "c"), after.map { it.packageName })
        assertEquals(listOf(0, 1, 2, 3), after.map { it.position })
        assertEquals(true, after.single { it.packageName == "h" }.hidden)
    }

    @Test
    fun `a system app with only a phone entry is hidden at first, a phone app the owner installed is not`() {
        val entries = listOf(
            LaunchEntry("org.droidtv.devicesetup", "Language", tv = false, system = true),
            LaunchEntry("com.android.tv.settings", "Settings", tv = true, system = true),
            LaunchEntry("com.example.phoneonly", "Phone app", tv = false, system = false),
            LaunchEntry("com.example.videos", "Videos (phone)", tv = false, system = false),
            LaunchEntry("com.example.videos", "Videos", tv = true, system = false),
            LaunchEntry("com.biskyt.mytvlauncher", "MyTVLauncher", tv = true, system = false)
        )
        assertEquals(
            listOf(
                InstalledApp("com.android.tv.settings", "Settings"),
                InstalledApp("com.example.videos", "Videos"),
                InstalledApp("org.droidtv.devicesetup", "Language", hiddenAtFirst = true),
                InstalledApp("com.example.phoneonly", "Phone app")
            ),
            LaunchEntries.pick(entries, own = "com.biskyt.mytvlauncher")
        )
    }

    @Test
    fun `an app hidden at first is added hidden, and once hidden again though it has a place`() {
        val setup = InstalledApp("org.droidtv.devicesetup", "Language", hiddenAtFirst = true)
        assertEquals(listOf(true), AppOrder.merge(listOf(setup), emptyList()).map { it.hidden })
        val saved = listOf(AppPlace(setup.packageName, 0))
        assertEquals(listOf(false), AppOrder.merge(listOf(setup), saved).map { it.hidden })
        assertEquals(listOf(true), AppOrder.merge(listOf(setup), saved, hideAtFirstAgain = true).map { it.hidden })
    }

    @Test
    fun `All apps saves every app in the order moved, hidden ones too, and an app not in it goes after`() {
        val places = listOf(AppPlace("a", 0), AppPlace("b", 1, hidden = true), AppPlace("c", 2), AppPlace("d", 3))
        val after = AppOrder.reorderAll(places, listOf("c", "b", "a"))
        assertEquals(listOf("c", "b", "a", "d"), after.map { it.packageName })
        assertEquals(listOf(0, 1, 2, 3), after.map { it.position })
        assertEquals(true, after[1].hidden)
    }
}
