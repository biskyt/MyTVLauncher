package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.BaseColumns
import androidx.tvprovider.media.tv.TvContractCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How many rows a table gave, or why it gave none ([error]); [count] is null when the TV has no such table. */
data class TableCount(val count: Int?, val error: String? = null)

/** What the Watch Next check found: whether the permission is given, and what the launcher can read. */
data class ListingsCheck(val allowed: Boolean, val watchNext: TableCount, val channels: TableCount)

/**
 * The Watch Next check in Settings: whether the launcher may read other apps' Watch Next items and home screen
 * channels, and how many it can read. Some TVs need the permission given with adb (see [ADB_GRANT]). Reads run in the
 * background and ask only for row ids, never in a way that holds up Home.
 */
class TvListings(private val context: Context) {
    suspend fun check(): ListingsCheck = withContext(Dispatchers.IO) {
        val allowed = context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
        ListingsCheck(allowed, count(TvContractCompat.WatchNextPrograms.CONTENT_URI), count(TvContractCompat.Channels.CONTENT_URI))
    }

    /**
     * The rows in [uri]. No selection and no sort order: Android 9 refuses both from an app that cannot read every
     * app's data (ACCESS_ALL_EPG_DATA, for system apps only).
     */
    private fun count(uri: Uri): TableCount = try {
        TableCount(context.contentResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)?.use { it.count })
    } catch (e: SecurityException) {
        refused(e)
    } catch (e: IllegalArgumentException) {
        refused(e) // a TV whose provider refuses the query
    }

    private fun refused(e: Exception) = TableCount(null, e.message ?: e.javaClass.simpleName)

    companion object {
        const val PERMISSION = "android.permission.READ_TV_LISTINGS"

        /** The command that gives the permission, for TVs that do not ask. */
        const val ADB_GRANT = "adb shell pm grant com.biskyt.mytvlauncher $PERMISSION"
    }
}
