package com.biskyt.mytvlauncher.ui.home

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The colours of Home's glows (the owner, 2026-11-15). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AmbientTest {
    @Test
    fun `the glows take the picture's top left, top right and lower half, brightened`() {
        val b = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        for (y in 0 until 24) {
            for (x in 0 until 24) {
                b.setPixel(
                    x,
                    y,
                    when {
                        y >= 12 -> AndroidColor.rgb(0, 60, 0)
                        x < 12 -> AndroidColor.rgb(80, 0, 0)
                        else -> AndroidColor.rgb(0, 0, 80)
                    }
                )
            }
        }
        val (topLeft, topRight, lower) = tonesOf(b).map { it!! }
        assertTrue("top left $topLeft", topLeft.red > 0.7f && topLeft.green < 0.1f)
        assertTrue("top right $topRight", topRight.blue > 0.7f && topRight.red < 0.1f)
        // Dark green, made bright enough to glow.
        assertTrue("lower $lower", lower.green > 0.5f && lower.red < 0.1f)
    }

    @Test
    fun `transparent pixels do not count`() {
        val b = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        b.eraseColor(AndroidColor.TRANSPARENT)
        for (y in 4 until 20) for (x in 4 until 20) b.setPixel(x, y, AndroidColor.rgb(200, 100, 0))
        tonesOf(b).forEach { assertTrue("$it", it!!.red > it.blue + 0.4f) }
    }

    @Test
    fun `a part with no opaque pixel gives no colour, for the accent's`() {
        val b = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        b.eraseColor(AndroidColor.TRANSPARENT)
        // Only the lower half has colour.
        for (y in 12 until 24) for (x in 0 until 24) b.setPixel(x, y, AndroidColor.rgb(0, 0, 200))
        val (topLeft, topRight, lower) = tonesOf(b)
        assertNull(topLeft)
        assertNull(topRight)
        assertTrue("$lower", lower!!.blue > 0.7f)
    }
}
