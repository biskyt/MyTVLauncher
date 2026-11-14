package com.biskyt.mytvlauncher.core.keys

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePressLogTest {
    @Test
    fun `each press has the gap since the last, newest first`() {
        val log = HomePressLog.add(HomePressLog.add(emptyList(), 1_000, inFront = false), 1_250, inFront = true)
        assertEquals(listOf(HomePress(1_250, 250, inFront = true, endsDouble = true), HomePress(1_000, null, inFront = false)), log)
    }

    @Test
    fun `only the newest presses are kept`() {
        val log = listOf(1L, 2L, 3L).fold(emptyList<HomePress>()) { list, at -> HomePressLog.add(list, at, inFront = true, keep = 2) }
        assertEquals(listOf(3L, 2L), log.map { it.atMs })
    }

    @Test
    fun `two presses within 500 ms make a double press`() {
        val one = HomePressLog.add(emptyList(), 1_000, inFront = false)
        assertEquals(false, HomePressLog.isDouble(one))
        assertEquals(true, HomePressLog.isDouble(HomePressLog.add(one, 1_350, inFront = true)))
        assertEquals(false, HomePressLog.isDouble(HomePressLog.add(one, 1_600, inFront = true)))
    }

    @Test
    fun `a third quick press is a single press`() {
        val two = HomePressLog.add(HomePressLog.add(emptyList(), 1_000, inFront = false), 1_200, inFront = true)
        assertEquals(false, HomePressLog.isDouble(HomePressLog.add(two, 1_400, inFront = true)))
    }
}
