package com.biskyt.mytvlauncher.ui.home

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size as CoilSize
import coil3.transform.Transformation
import com.biskyt.mytvlauncher.core.watchnext.LogoLook
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A TV channel's logo on its card (the owner, 2026-11-15: logos touched the card's edges and were of many sizes). The
 * logo's transparent border is cut off, a dark logo's dark parts are made white ([LogoTrim]), and every logo gets
 * about the same area inside a margin ([LogoFit]). Every logo is on one dark, see-through card ([LOGO_TILE]; the
 * owner, 2026-11-24, as MyTVPlayer 1.36.0): a light card hid white logos, and a mix of light and dark cards looked bad.
 * The work is done once for each picture, off the main thread, and Coil keeps the result.
 */
@Composable
fun LogoPicture(uri: String?, width: Dp, height: Dp, placeholder: @Composable () -> Unit) {
    var loaded by remember(uri) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(LOGO_TILE)) {
        if (!loaded) placeholder()
        if (uri == null) return@Box
        val context = LocalContext.current
        val density = LocalDensity.current
        val request = remember(uri, width, height) {
            with(density) {
                // Decoded at twice the card's size, so that a trimmed logo stays sharp when LogoFit scales it up. Not
                // RGB 565, which has no transparency.
                ImageRequest.Builder(context).data(uri).size(2 * width.roundToPx(), 2 * height.roundToPx())
                    .transformations(LogoTrim).build()
            }
        }
        AsyncImage(
            request,
            contentDescription = null,
            contentScale = LogoFit,
            onSuccess = { loaded = true },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Cuts off a logo's transparent border, and makes a dark logo's dark parts white (see [LogoLook]). */
internal object LogoTrim : Transformation() {
    override val cacheKey = "logo-trim-3"

    override suspend fun transform(input: Bitmap, size: CoilSize): Bitmap {
        // A picture with no transparency has no border to cut, and its own background: left as it is.
        if (!input.hasAlpha()) return input
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        val look = LogoLook.of(pixels, input.width, input.height) ?: return input
        if (!look.dark) {
            val whole = look.width == input.width && look.height == input.height
            return if (whole) input else Bitmap.createBitmap(input, look.left, look.top, look.width, look.height)
        }
        LogoLook.lighten(pixels)
        val out = Bitmap.createBitmap(look.width, look.height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, look.top * input.width + look.left, input.width, 0, 0, look.width, look.height)
        return out
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

private const val MARGIN = 0.1f
private const val AREA = 0.4f

/** The card behind every logo: black, a little see-through, so it is darker than what is behind it and tinted by it. */
internal val LOGO_TILE = Color.Black.copy(alpha = 0.4f)
