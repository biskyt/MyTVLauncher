package com.biskyt.mytvlauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import android.view.KeyEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.hasContentDescription
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.MainActivity
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.core.channels.AppChannel
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.iconBitmap
import com.biskyt.mytvlauncher.testutil.FakeApps
import com.biskyt.mytvlauncher.testutil.FakeChannels
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import com.biskyt.mytvlauncher.testutil.createUiComposeRule
import com.biskyt.mytvlauncher.testutil.sendKey
import com.biskyt.mytvlauncher.testutil.waitAdvancingClock
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Google Play listing's graphics and screenshots, drawn from the app itself so that they match it. Only run when
 * asked: `./gradlew :app:testDebugUnitTest --tests '*StoreAssetsTest*' -PstoreAssets`. The files go to store/ at the
 * repository root. The apps, posters and channel logos are stand-ins drawn here: no real app, title or broadcaster
 * shows.
 *
 * xhdpi on a 960 × 540 dp screen gives the 1920 × 1080 pixels that Play asks for in TV screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w960dp-h540dp-xhdpi")
class StoreAssetsTest {
    @get:Rule val rule = createUiComposeRule<MainActivity>()

    private val out = File(OUT)

    @Test
    fun screenshots() {
        val c = rule.activity.container

        @Suppress("UNCHECKED_CAST")
        val cache = c.icons.javaClass.getDeclaredField("cache").apply { isAccessible = true }.get(c.icons) as LruCache<String, ImageBitmap>
        APPS.forEach { cache.put(it.packageName, icon(it.colour, it.glyph)) }
        FakeApps.apps = APPS.map { (pkg, label) -> InstalledApp(pkg, label) }
        FakeWatchNext.items = watchNext()
        FakeChannels.channels = channels()
        runBlocking {
            c.apps.look()
            c.watchNext.read()
            c.channels.read()
        }
        rule.waitAdvancingClock { rule.onAllNodes(hasContentDescription("News 24")).fetchSemanticsNodes().isNotEmpty() }
        // Home read the first apps' icons before the stand-ins were in memory: it reads them again when it comes back.
        open(Screen.SETTINGS)
        shot("5-settings")
        back()
        shot("1-home")
        // Down to the channel rows.
        repeat(2) {
            rule.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN)
            rule.sendKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_UP)
        }
        shot("2-channels")
        runBlocking { c.apps.setHidden("com.example.games", true) }
        open(Screen.ALL_APPS)
        shot("3-all-apps")
        back()
        open(Screen.RECENTS)
        shot("4-recent-apps")
    }

    /** Play's app icon: the launcher's icon as its square (Play rounds the corners itself). 32-bit, as Play asks. */
    @Test
    fun icon() {
        val drawable = ContextCompat.getDrawable(rule.activity, R.mipmap.ic_launcher)!!
        save("graphics/icon-512", iconBitmap(drawable, 512), flat = false)
    }

    /** The Android TV banner: the app's own banner, at Play's size. */
    @Test
    fun tvBanner() {
        val banner = ContextCompat.getDrawable(rule.activity, R.drawable.banner)!!.toBitmap(1280, 720)
        val canvas = Canvas(banner)
        // The app's name under the symbol, as Play asks a TV banner to show it.
        canvas.drawText(NAME, 640f, 625f, text(72f, Paint.Align.CENTER))
        save("graphics/tv-banner-1280x720", banner)
    }

    /** The feature graphic: the symbol, the name and a line about the app. */
    @Test
    fun featureGraphic() {
        val bitmap = Bitmap.createBitmap(1024, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)
        canvas.drawPaint(
            Paint().apply { shader = LinearGradient(0f, 0f, 1024f, 500f, 0x332BC4B0, 0x00000000, Shader.TileMode.CLAMP) }
        )
        // The banner's symbol: four tiles in the accent colour.
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f).forEach { (x, y) ->
            val left = 110f + x * 100f
            val top = 150f + y * 110f
            canvas.drawRoundRect(left, top, left + 84f, top + 94f, 14f, 14f, tile)
        }
        canvas.drawText(NAME, 350f, 225f, text(80f, Paint.Align.LEFT))
        canvas.drawText(TAGLINE, 354f, 290f, text(34f, Paint.Align.LEFT, MUTED))
        canvas.drawText(TAGLINE_2, 354f, 336f, text(34f, Paint.Align.LEFT, MUTED))
        save("graphics/feature-graphic-1024x500", bitmap)
    }

    private fun text(size: Float, align: Paint.Align, colour: Int = TEXT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colour
        textSize = size
        textAlign = align
        typeface = Typeface.create(ResourcesCompat.getFont(rule.activity, R.font.nunito), BOLD, false)
    }

    private fun open(screen: Screen) = rule.runOnUiThread { rule.activity.nav.open(screen) }

    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    /** The screen as it is now, once the background and the pictures have settled. */
    private fun shot(name: String) {
        // The frame clock as well as the main looper: the screen draws on the frames after a change.
        repeat(40) {
            rule.waitAdvancingClock(20) { true }
            rule.mainClock.advanceTimeBy(20)
        }
        lateinit var image: Bitmap
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
        }
        save("screenshots/$name", image)
    }

    /** Saves [bitmap] as a PNG; [flat]: with no transparency (24-bit), as Play asks for all but the icon. */
    private fun save(name: String, bitmap: Bitmap, flat: Boolean = true) {
        val image = if (flat) {
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply { drawColor(BACKGROUND) }.drawBitmap(bitmap, 0f, 0f, null)
                it.setHasAlpha(false)
            }
        } else {
            bitmap
        }
        val file = File(out, "$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** A stand-in app icon: a colour with a symbol. */
    private fun icon(colour: Int, glyph: String): ImageBitmap {
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colour)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT
            textSize = 92f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(glyph, 96f, 96f - (paint.descent() + paint.ascent()) / 2, paint)
        return bitmap.asImageBitmap()
    }

    /** A stand-in picture file: a gradient from [top] to [bottom] with shapes and a title; [wide]: 16:9, else 2:3. */
    private fun poster(name: String, wide: Boolean, top: Int, bottom: Int, title: String): String {
        val w = if (wide) 662 else 248
        val h = 372
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP) })
        val shape = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
        canvas.drawCircle(w * 0.7f, h * 0.35f, w * 0.35f, shape)
        canvas.drawCircle(w * 0.25f, h * 0.6f, w * 0.22f, shape)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT
            textSize = w / 9f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title.uppercase(), w / 2f, h * 0.86f, paint)
        return file(name, bitmap)
    }

    /** A stand-in TV channel logo: a mark and text on a transparent ground; [dark]: black text, which shows white. */
    private fun logo(name: String, text: String, colour: Int, dark: Boolean = false): String {
        val bitmap = Bitmap.createBitmap(400, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRoundRect(10f, 30f, 110f, 130f, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) 0xFF111111.toInt() else TEXT
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(text, 130f, 105f, paint)
        return file(name, bitmap)
    }

    private fun file(name: String, bitmap: Bitmap): String {
        val file = File.createTempFile("store-$name", ".png").apply { deleteOnExit() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.toURI().toString()
    }

    private fun watchNext(): List<WatchNextItem> {
        val v = VIDEOS
        return listOf(
            WatchNextItem(
                "w1", v, "Sintel", null, poster("p1", false, 0xFF0D47A1.toInt(), 0xFF26C6DA.toInt(), "Sintel"),
                CardShape.POSTER, 0.45f, 9, null, leftMs = 8 * MINUTE
            ),
            WatchNextItem(
                "w2", v, "Tears of Steel", "Part 2",
                poster("p2", true, 0xFF263238.toInt(), 0xFFFF7043.toInt(), "Tears of Steel"),
                CardShape.WIDE, 0.7f, 8, null, "1", "2", 4 * MINUTE
            ),
            WatchNextItem(
                "w3", v, "Big Buck Bunny", null, poster("p3", false, 0xFF33691E.toInt(), 0xFFFFEE58.toInt(), "Bunny"),
                CardShape.POSTER, null, 7, null, leftMs = 10 * MINUTE
            ),
            WatchNextItem(
                "w4", v, "Elephants Dream", null, poster("p4", false, 0xFF4A148C.toInt(), 0xFFFF80AB.toInt(), "Dream"),
                CardShape.POSTER, 0.2f, 6, null, leftMs = 9 * MINUTE
            ),
            WatchNextItem(
                "w5", v, "Spring", null, poster("p5", true, 0xFF1B5E20.toInt(), 0xFFA5D6A7.toInt(), "Spring"),
                CardShape.WIDE, 0.3f, 5, null, leftMs = 6 * MINUTE
            ),
        )
    }

    private fun channels(): List<AppChannel> {
        val v = VIDEOS
        val m = "com.example.music"
        return listOf(
            AppChannel(
                7,
                v,
                "Favourite channels",
                listOf(
                    WatchNextItem("c1", v, "News 24", null, logo("l1", "NEWS 24", 0xFFC62828.toInt()), CardShape.LOGO, null, 4, null),
                    WatchNextItem("c2", v, "Sport One", null, logo("l2", "SPORT 1", 0xFF2E7D32.toInt()), CardShape.LOGO, null, 3, null),
                    WatchNextItem(
                        "c3", v, "Kids Club", null, logo("l3", "KIDS", 0xFFFFB300.toInt(), dark = true), CardShape.LOGO, null, 2, null
                    ),
                    WatchNextItem("c4", v, "Movies+", null, logo("l4", "MOVIES+", 0xFF6A1B9A.toInt()), CardShape.LOGO, null, 1, null),
                )
            ),
            AppChannel(
                9,
                m,
                "Recently played",
                listOf(
                    WatchNextItem(
                        "m1", m, "Night Drive", null, poster("q1", true, 0xFF311B92.toInt(), 0xFFE040FB.toInt(), "Night Drive"),
                        CardShape.WIDE, null, 2, null
                    ),
                    WatchNextItem(
                        "m2", m, "Morning Light", null, poster("q2", true, 0xFFE65100.toInt(), 0xFFFFE082.toInt(), "Morning"),
                        CardShape.WIDE, null, 1, null
                    ),
                )
            ),
        )
    }

    private data class DemoApp(val packageName: String, val label: String, val colour: Int, val glyph: String)

    private companion object {
        const val OUT = "../store"
        const val NAME = "MyTVLauncher"
        const val TAGLINE = "A light home screen for your TV."
        const val TAGLINE_2 = "No ads. No tracking."
        const val ACCENT = 0xFF2BC4B0.toInt()
        const val BOLD = 700
        const val VIDEOS = "com.example.videos"
        const val MINUTE = 60_000L
        const val BACKGROUND = 0xFF0F1114.toInt()
        const val TEXT = 0xFFF1F3F6.toInt()
        const val MUTED = 0xFFBCC2CB.toInt()

        val APPS = listOf(
            DemoApp(VIDEOS, "Films", 0xFFE53935.toInt(), "▶"),
            DemoApp("com.example.music", "Music", 0xFF8E24AA.toInt(), "♪"),
            DemoApp("com.example.photos", "Photos", 0xFF43A047.toInt(), "✿"),
            DemoApp("com.example.browser", "Browser", 0xFF1E88E5.toInt(), "◎"),
            DemoApp("com.example.news", "News", 0xFFF4511E.toInt(), "N"),
            DemoApp("com.example.weather", "Weather", 0xFF00ACC1.toInt(), "☀"),
            DemoApp("com.example.games", "Games", 0xFF3949AB.toInt(), "◆"),
            DemoApp("com.android.tv.settings", "Settings", 0xFF546E7A.toInt(), "⚙"),
        )
    }
}
