package com.biskyt.mytvlauncher.data

import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** An app's icon on its round card (the owner, 2026-11-15). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppIconsTest {
    @Test
    fun `an adaptive icon's background reaches every edge, with no shape mask`() {
        val icon = AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.TRANSPARENT))
        val bitmap = iconBitmap(icon, 64)
        assertEquals(64, bitmap.width)
        // The corners too: a masked icon (a circle or a rounded square) leaves them transparent.
        for ((x, y) in listOf(0 to 0, 63 to 0, 0 to 63, 63 to 63, 32 to 32)) assertEquals(Color.RED, bitmap.getPixel(x, y))
    }

    @Test
    fun `any other icon is drawn as it is`() {
        val bitmap = iconBitmap(ColorDrawable(Color.BLUE), 32)
        assertEquals(Color.BLUE, bitmap.getPixel(0, 0))
    }
}
