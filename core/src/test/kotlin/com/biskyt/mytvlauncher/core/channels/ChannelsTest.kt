package com.biskyt.mytvlauncher.core.channels

import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelsTest {
    private fun item(key: String, weight: Long) = WatchNextItem(key, "app", key, null, null, CardShape.WIDE, null, weight, null)

    @Test
    fun `channels turned off or with no items are left out, in the TV's order`() {
        val channels =
            listOf(
                AppChannel(3, "a", "Films", listOf(item("x", 1))),
                AppChannel(1, "b", "News", emptyList()),
                AppChannel(2, "c", "Top", listOf(item("y", 1)))
            )
        assertEquals(listOf(2L), Channels.select(channels, hiddenIds = setOf(3L)).map { it.id })
    }

    @Test
    fun `a channel's items go highest weight first, at most 30`() {
        val items = (1L..40L).map { item("k$it", it) }
        val ordered = Channels.order(items)
        assertEquals(30, ordered.size)
        assertEquals("k40", ordered.first().key)
    }

    @Test
    fun `items with one key are kept once, before the cut, so the row stays full`() {
        val items = (1L..40L).map { item(if (it > 35) "same" else "k$it", it) }
        val ordered = Channels.order(items)
        assertEquals(30, ordered.size)
        assertEquals(30, ordered.map { it.key }.distinct().size)
    }
}
