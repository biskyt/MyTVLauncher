package com.biskyt.mytvlauncher.core.watchnext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchNextTest {
    private fun item(key: String, app: String = "com.example.videos", at: Long = 0) =
        WatchNextItem(key, app, key, null, null, CardShape.WIDE, null, at, null)

    @Test
    fun `newest first, at most the limit`() {
        val items = (1L..35L).map { item("k$it", at = it) }
        val shown = WatchNext.select(items, emptySet(), emptySet())
        assertEquals(30, shown.size)
        assertEquals("k35", shown.first().key)
        assertEquals("k6", shown.last().key)
        assertEquals(10, WatchNext.select(items, emptySet(), emptySet(), limit = 10).size)
    }

    @Test
    fun `the limit steps by 5 from 10 to 40, and goes round`() {
        assertEquals(35, WatchNext.stepLimit(30, 1))
        assertEquals(25, WatchNext.stepLimit(30, -1))
        assertEquals(10, WatchNext.stepLimit(40, 1))
        assertEquals(40, WatchNext.stepLimit(10, -1))
        // A saved value that is not a choice shows as the default, so a step goes from there.
        assertEquals(35, WatchNext.stepLimit(7, 1))
    }

    @Test
    fun `a limit that is not a choice gives the default`() {
        assertEquals(30, WatchNext.validLimit(7))
        assertEquals(30, WatchNext.validLimit(20 + 1))
        assertEquals(15, WatchNext.validLimit(15))
    }

    @Test
    fun `hidden apps and hidden items are left out, before the limit`() {
        val items = listOf(item("a", at = 3), item("b", app = "com.example.news", at = 2), item("c", at = 1))
        assertEquals(listOf("c"), WatchNext.select(items, setOf("com.example.news"), setOf("a")).map { it.key })
        assertEquals(listOf("a"), WatchNext.select(items, setOf("com.example.news"), emptySet(), limit = 1).map { it.key })
    }

    @Test
    fun `a tall poster makes a poster card, anything else a wide card`() {
        assertEquals("p" to CardShape.POSTER, WatchNext.picture("p", 4, "t"))
        assertEquals("p" to CardShape.POSTER, WatchNext.picture("p", 5, "t"))
        assertEquals("p" to CardShape.WIDE, WatchNext.picture("p", 0, "t"))
        assertEquals("p" to CardShape.WIDE, WatchNext.picture("p", null, "t"))
        assertEquals("t" to CardShape.WIDE, WatchNext.picture(null, 4, "t"))
        assertEquals(null to CardShape.WIDE, WatchNext.picture(null, null, null))
    }

    @Test
    fun `a TV channel's picture is a logo`() {
        assertEquals("logo" to CardShape.LOGO, WatchNext.picture("logo", 1, "thumb", type = 6))
        assertEquals("thumb" to CardShape.LOGO, WatchNext.picture(null, null, "thumb", type = 6))
        assertEquals("logo" to CardShape.WIDE, WatchNext.picture("logo", 1, "thumb", type = 0))
    }

    @Test
    fun `progress needs a position and a duration`() {
        assertEquals(0.25f, WatchNext.progress(250, 1000)!!, 0.001f)
        assertEquals(1f, WatchNext.progress(2000, 1000)!!, 0.001f)
        assertNull(WatchNext.progress(0, 1000))
        assertNull(WatchNext.progress(250, null))
    }

    @Test
    fun `the time left is the whole length before the item is started, else what is left`() {
        assertEquals(1000L, WatchNext.left(null, 1000))
        assertEquals(1000L, WatchNext.left(0, 1000))
        assertEquals(750L, WatchNext.left(250, 1000))
        assertNull(WatchNext.left(1000, 1000))
        assertNull(WatchNext.left(250, null))
        assertNull(WatchNext.left(250, 0))
    }

    @Test
    fun `the key uses the app's own id when it has one`() {
        assertEquals("com.example.videos/ep-7", WatchNext.key("com.example.videos", "ep-7", 12))
        assertEquals("com.example.videos/#12", WatchNext.key("com.example.videos", " ", 12))
        assertEquals("com.example.videos/#12", WatchNext.key("com.example.videos", null, 12))
    }
}
