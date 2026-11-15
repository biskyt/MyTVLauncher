package com.biskyt.mytvlauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import com.biskyt.mytvlauncher.ui.theme.Palette
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Home's background follows the card with focus (the owner, 2026-11-15). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AmbientUiTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    /** The colour of the screen's top left corner, where the backdrop shows and a glow bleeds in. */
    private fun corner(): Int {
        var pixel = 0
        rule.runOnUiThread {
            val v = rule.activity.window.decorView
            val img = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(img))
            pixel = img.getPixel(2, 2)
        }
        return pixel
    }

    private fun focused(description: String) = runCatching { rule.onNodeWithContentDescription(description).assertIsFocused() }.isSuccess

    @Test
    fun `a red picture that comes to the card with focus tints the background red, and an app's glow takes over after`() {
        val red = File.createTempFile("red", ".png").apply {
            val b = Bitmap.createBitmap(124, 186, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // The card takes focus with no picture (as from saved data at start-up), and the background settles on that.
        rule.waitAdvancingClock { focused(FakeWatchNext.film.title) }
        var steps = 0
        rule.waitAdvancingClock(stepMs = 50) { ++steps > 10 }
        // Then the card gets its picture while it keeps the focus: the backdrop, red under the dark overlay, follows.
        FakeWatchNext.items = listOf(FakeWatchNext.film.copy(pictureUri = red.toURI().toString()))
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitAdvancingClock(stepMs = 50) { corner().let { Color.red(it) > Color.green(it) + 20 } }

        rule.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN)
        rule.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_UP)
        rule.waitAdvancingClock { focused("Browser") }
        // The test apps have no icons: the accent's glow (mint), no longer red.
        rule.waitAdvancingClock(stepMs = 50) { corner().let { Color.green(it) > Color.red(it) } }
    }

    @Test
    fun `with the background brightness at 0, the background stays plain and dark`() {
        runBlocking { rule.activity.container.settings.setBackdropLevel(0) }
        rule.waitAdvancingClock { rule.activity.container.backdropLevel.value == 0 }
        FakeWatchNext.items = emptyList()
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitAdvancingClock { focused("Browser") }
        // Past the settle delay and the fade (40 steps of 50 ms): still the plain background.
        var steps = 0
        rule.waitAdvancingClock(stepMs = 50) { ++steps > 40 }
        assertEquals(Palette.background.toArgb(), corner())
    }
}
