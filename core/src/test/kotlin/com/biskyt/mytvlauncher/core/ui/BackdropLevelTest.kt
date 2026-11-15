package com.biskyt.mytvlauncher.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BackdropLevelTest {
    @Test
    fun `Left and Right step by 5 and stop at 0 and 40`() {
        assertEquals(25, BackdropLevel.step(20, 1))
        assertEquals(15, BackdropLevel.step(20, -1))
        assertEquals(0, BackdropLevel.step(0, -1))
        assertEquals(40, BackdropLevel.step(40, 1))
    }

    @Test
    fun `the overlay and the glows follow the level`() {
        assertEquals(0.8f, BackdropLevel.overlayAlpha(20), 0.001f)
        assertEquals(1f, BackdropLevel.overlayAlpha(0), 0.001f)
        assertEquals(1f, BackdropLevel.glowScale(28), 0.001f)
        assertEquals(0f, BackdropLevel.glowScale(0), 0.001f)
    }

    @Test
    fun `a value that is not a choice gives the default`() {
        assertEquals(20, BackdropLevel.valid(7))
        assertEquals(35, BackdropLevel.valid(35))
    }
}
