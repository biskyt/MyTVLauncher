package com.biskyt.mytvlauncher.ui.home

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.size.Size as CoilSize
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A TV channel's logo on its card (the owner, 2026-11-15). */
@RunWith(AndroidJUnit4::class)
class LogoTest {
    private val card = Size(330f, 186f)

    private fun drawn(logo: Size): Size = LogoFit.computeScaleFactor(logo, card).let {
        Size(logo.width * it.scaleX, logo.height * it.scaleY)
    }

    @Test
    fun `a logo keeps clear of the card's edges`() {
        // A logo of the card's own shape, as BBC One's: inside the margin, not to the edges.
        val d = drawn(Size(660f, 372f))
        assertTrue(d.width <= card.width * 0.72f + 1)
        assertTrue(d.height <= card.height * 0.72f + 1)
    }

    @Test
    fun `a wide logo and a square one get about the same area`() {
        val wide = drawn(Size(300f, 100f))
        val square = drawn(Size(50f, 50f))
        val ratio = (wide.width * wide.height) / (square.width * square.height)
        assertTrue("area ratio $ratio", ratio in 0.8f..1.25f)
    }

    @Test
    fun `the transparent border is cut off, and black text on it is dark`() {
        val bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            // Black letters: strokes with gaps between them, as text has.
            for (x in 10 until 30) for (y in 5 until 15) if (x % 3 != 2 || x == 29) setPixel(x, y, Color.BLACK)
        }
        val trimmed = runBlocking { LogoTrim("dark").transform(bitmap, CoilSize.ORIGINAL) }
        assertEquals(20, trimmed.width)
        assertEquals(10, trimmed.height)
        assertEquals(true, LogoTrim.isDark("dark"))
        assertNull(LogoTrim.isDark("not seen"))
        // Looked at again, as after Coil's memory cache gave it and the note was dropped.
        assertTrue(LogoTrim.look("again", trimmed))
    }
}
