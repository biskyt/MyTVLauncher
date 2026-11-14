package com.biskyt.mytvlauncher.core.rows

import org.junit.Assert.assertEquals
import org.junit.Test

class RowOrderTest {
    private val rows = listOf(RowOrder.WATCH_NEXT, RowOrder.APPS, RowOrder.channel(1), RowOrder.channel(2))

    @Test
    fun `with nothing saved, Home keeps its usual order`() {
        assertEquals(rows, RowOrder.order(rows, emptyList()))
    }

    @Test
    fun `the saved order wins, and a new channel goes after the saved rows`() {
        val saved = listOf(RowOrder.APPS, RowOrder.channel(2), RowOrder.WATCH_NEXT)
        assertEquals(
            listOf(RowOrder.APPS, RowOrder.channel(2), RowOrder.WATCH_NEXT, RowOrder.channel(1)),
            RowOrder.order(rows, saved)
        )
    }

    @Test
    fun `saving keeps the rows not shown now after the moved ones`() {
        val saved = listOf(RowOrder.channel(9), RowOrder.WATCH_NEXT, RowOrder.APPS)
        assertEquals(
            listOf(RowOrder.APPS, RowOrder.WATCH_NEXT, RowOrder.channel(9)),
            RowOrder.save(listOf(RowOrder.APPS, RowOrder.WATCH_NEXT), saved)
        )
    }
}
