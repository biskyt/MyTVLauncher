package com.biskyt.mytvlauncher.ui.home

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size as CoilSize
import coil3.transform.Transformation
import com.biskyt.mytvlauncher.core.watchnext.LogoLook
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A TV channel's logo on its card (the owner, 2026-11-15: logos touched the card's edges, were of many sizes, and black
 * text was lost on the dark card). The logo's transparent border is cut off ([LogoTrim]), every logo gets about the
 * same area inside a margin ([LogoFit]), and a dark logo on a transparent background goes on a light card. The work is
 * done once for each picture, off the main thread, and Coil keeps the result.
 */
@Composable
fun LogoPicture(uri: String?, width: Dp, height: Dp, placeholder: @Composable () -> Unit) {
    var loaded by remember(uri) { mutableStateOf(false) }
    var dark by remember(uri) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(if (dark) LIGHT_CARD else Color.Transparent)) {
        if (!loaded) placeholder()
        if (uri == null) return@Box
        val context = LocalContext.current
        val density = LocalDensity.current
        val request = remember(uri, width, height) {
            with(density) {
                // Decoded at the card's size, not the app's full size. Not RGB 565, which has no transparency.
                ImageRequest.Builder(context).data(uri).size(width.roundToPx(), height.roundToPx())
                    .transformations(LogoTrim(uri)).build()
            }
        }
        AsyncImage(
            request,
            contentDescription = null,
            contentScale = LogoFit,
            onSuccess = { state ->
                loaded = true
                val known = LogoTrim.isDark(uri)
                if (known != null) {
                    dark = known
                } else {
                    // From Coil's memory cache after this note was dropped: looked at again, off the main thread.
                    val bitmap = (state.result.image as? BitmapImage)?.bitmap
                    if (bitmap != null) scope.launch { dark = withContext(Dispatchers.Default) { LogoTrim.look(uri, bitmap) } }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Cuts off a logo's transparent border, and notes whether it is dark (see [LogoLook]). */
internal class LogoTrim(private val uri: String) : Transformation() {
    override val cacheKey = "logo-trim-1"

    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap {
        if (!input.hasAlpha()) return input
        val look = lookAt(uri, input) ?: return input
        if (look.width == input.width && look.height == input.height) return input
        return Bitmap.createBitmap(input, look.left, look.top, look.width, look.height)
    }

    companion object {
        /**
         * Whether each logo is dark, by its address. Coil's memory cache keeps the trimmed picture, not this, so it is
         * kept here: a few hundred booleans at most. A logo no longer here is looked at again (see LogoPicture).
         */
        private val dark = LruCache<String, Boolean>(LOGOS_KEPT)

        /** Whether the logo at [uri] is dark, or null when it is not known (any more). */
        fun isDark(uri: String): Boolean? = dark.get(uri)

        /** Looks at [bitmap], the logo at [uri], and notes whether it is dark. */
        fun look(uri: String, bitmap: Bitmap): Boolean = lookAt(uri, bitmap)?.dark ?: false

        private fun lookAt(uri: String, bitmap: Bitmap): LogoLook? {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return LogoLook.of(pixels, bitmap.width, bitmap.height)?.also { dark.put(uri, it.dark) }
        }
    }
}

/**
 * Gives every logo about the same area ([AREA] of the card's), so that a wide logo and a square one look as large as
 * each other, and keeps it inside a margin ([MARGIN] of the card's width and height on each side).
 */
internal object LogoFit : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        if (srcSize.width <= 0f || srcSize.height <= 0f) return ScaleFactor(1f, 1f)
        val inside = min(dstSize.width * (1 - 2 * MARGIN) / srcSize.width, dstSize.height * (1 - 2 * MARGIN) / srcSize.height)
        val sameArea = sqrt(AREA * dstSize.width * dstSize.height / (srcSize.width * srcSize.height))
        val scale = min(inside, sameArea)
        return ScaleFactor(scale, scale)
    }
}

private const val MARGIN = 0.14f
private const val AREA = 0.3f
private const val LOGOS_KEPT = 512

/** The card behind a dark logo: light enough for black text, a little softer than white. */
private val LIGHT_CARD = Color(0xFFE6E8EB)
