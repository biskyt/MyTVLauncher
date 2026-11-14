package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.pm.PackageManager
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
            context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
                .also { cache.put(packageName, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private companion object {
        const val MAX_ICONS = 100
    }
}
