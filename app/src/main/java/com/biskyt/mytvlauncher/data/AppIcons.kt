package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Apps' icons, drawn once at the size Home shows them and kept in memory. They are read in the background, card by
 * card, so Home's first frame never waits for one.
 */
class AppIcons(private val context: Context, private val sizePx: Int) {
    private val cache = LruCache<String, ImageBitmap>(MAX_ICONS)

    /** The icon if it is in memory already. */
    fun cached(packageName: String): ImageBitmap? = cache.get(packageName)

    /** The icon, read now if it is not in memory; null if the app has gone. */
    suspend fun load(packageName: String): ImageBitmap? = cached(packageName) ?: withContext(Dispatchers.IO) {
        try {
            iconBitmap(context.packageManager.getApplicationIcon(packageName), sizePx).asImageBitmap()
                .also { cache.put(packageName, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private companion object {
        const val MAX_ICONS = 100
    }
}

/**
 * An app's icon as a [sizePx] square that the round app card fills (the owner, 2026-11-15). An adaptive icon (most
 * current apps') is drawn as its layers with no shape mask, its safe zone (the middle 72 of its 108 units) filling the
 * square, so its background reaches every edge of the circle. Any other icon is drawn as it is.
 */
internal fun iconBitmap(drawable: Drawable, sizePx: Int): Bitmap {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || drawable !is AdaptiveIconDrawable) return drawable.toBitmap(sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // The layers are 108 units with the safe zone in the middle 72: a quarter of the square more on each side.
    val extra = sizePx / 4
    listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
        layer.setBounds(-extra, -extra, sizePx + extra, sizePx + extra)
        layer.draw(canvas)
    }
    return bitmap
}
