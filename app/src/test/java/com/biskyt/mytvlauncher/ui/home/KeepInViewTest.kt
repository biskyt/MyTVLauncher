package com.biskyt.mytvlauncher.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class KeepInViewTest {
    @Test
    fun `an item already in view with its margin does not scroll`() {
        assertEquals(0f, keepInView(start = 100f, end = 200f, from = 0f, to = 1000f, margin = 50f))
    }

    @Test
    fun `an item past the end scrolls just far enough to show it with its margin`() {
        assertEquals(150f, keepInView(start = 900f, end = 1100f, from = 0f, to = 1000f, margin = 50f))
    }

    @Test
    fun `an item before the start scrolls back just far enough`() {
        assertEquals(-80f, keepInView(start = -30f, end = 70f, from = 0f, to = 1000f, margin = 50f))
    }

    @Test
    fun `an item too large for the space goes to the start`() {
        assertEquals(250f, keepInView(start = 300f, end = 1300f, from = 0f, to = 1000f, margin = 50f))
    }
}
