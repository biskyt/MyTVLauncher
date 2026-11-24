package com.biskyt.mytvlauncher.core.watchnext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogoLookTest {
    private val clear = 0x00000000
    private val white = 0xFFFFFFFF.toInt()

    /** A [w] by [h] transparent picture, with the colour that [fill] gives for each pixel it fills. */
    private fun picture(w: Int, h: Int, fill: (Int, Int) -> Int?) = IntArray(w * h) { i -> fill(i % w, i / w) ?: clear }

    @Test
    fun `the transparent border is cut off`() {
        val pixels = picture(10, 6) { x, y -> if (x in 2..7 && y in 1..3) white else null }
        val look = LogoLook.of(pixels, 10, 6)!!
        assertEquals(LogoLook(2, 1, 8, 4), look)
        assertEquals(6, look.width)
        assertEquals(3, look.height)
    }

    @Test
    fun `an all transparent picture has no logo`() {
        assertNull(LogoLook.of(IntArray(12) { clear }, 4, 3))
    }
}
