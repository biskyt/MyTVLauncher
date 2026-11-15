package com.biskyt.mytvlauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import java.io.File
import kotlinx.coroutines.runBlocking
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
    fun `a red card's picture tints the background red, and an app's glow takes over when the focus moves to it`() {
        val red = File.createTempFile("red", ".png").apply {
            val b = Bitmap.createBitmap(124, 186, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        FakeWatchNext.items = listOf(FakeWatchNext.film.copy(pictureUri = red.toURI().toString()))
        runBlocking { rule.activity.container.watchNext.read() }
        rule.waitAdvancingClock { focused(FakeWatchNext.film.title) }
        // The backdrop: red under the dark overlay.
        rule.waitAdvancingClock(stepMs = 50) { corner().let { Color.red(it) > Color.green(it) + 20 } }

        rule.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN)
        rule.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_UP)
        rule.waitAdvancingClock { focused("Browser") }
        // The test apps have no icons: the accent's glow (mint), no longer red.
        rule.waitAdvancingClock(stepMs = 50) { corner().let { Color.green(it) > Color.red(it) } }
    }
}
