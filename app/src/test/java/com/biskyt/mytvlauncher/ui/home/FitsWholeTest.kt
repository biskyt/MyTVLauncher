package com.biskyt.mytvlauncher.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitsWholeTest {
    @Test
    fun `a picture about the card's shape is cropped to fill it`() {
        assertFalse(fitsWhole(pictureAspect = 16f / 9, cardAspect = 331f / 186))
        assertFalse(fitsWhole(pictureAspect = 2f / 3, cardAspect = 124f / 186))
        // A 4:3 video thumbnail, as SmartTube gives, on a wide card.
        assertFalse(fitsWhole(pictureAspect = 4f / 3, cardAspect = 331f / 186))
    }

    @Test
    fun `a channel logo of another shape is shown whole`() {
        // A square logo on a wide card, and a wide logo on a poster card.
        assertTrue(fitsWhole(pictureAspect = 1f, cardAspect = 331f / 186))
        assertTrue(fitsWhole(pictureAspect = 4f, cardAspect = 331f / 186))
        assertTrue(fitsWhole(pictureAspect = 16f / 9, cardAspect = 124f / 186))
    }
}
