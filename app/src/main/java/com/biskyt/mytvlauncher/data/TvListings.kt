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

/**
 * What the Watch Next check found: whether the permission is given, and what the launcher can read. The channels table
 * holds apps' home screen channels ([appChannels], type preview) and the TV's own broadcast channels ([tvChannels]).
 */
data class ListingsCheck(val allowed: Boolean, val watchNext: TableCount, val appChannels: TableCount, val tvChannels: TableCount)

/**
 * The Watch Next check in Settings: whether the launcher may read other apps' Watch Next items and home screen
 * channels, and how many it can read. Some TVs need the permission given with adb (see [adbGrant]). Reads run in the
 * background and ask only for row ids, never in a way that holds up Home.
 */
class TvListings(private val context: Context) {
    suspend fun check(): ListingsCheck = withContext(Dispatchers.IO) {
        val watchNext = query(TvContractCompat.WatchNextPrograms.CONTENT_URI, null) { count, _ -> TableCount(count) to TableCount(count) }
        val channels = query(TvContractCompat.Channels.CONTENT_URI, TvContractCompat.Channels.COLUMN_TYPE) { count, types ->
            val preview = types.count { it == TvContractCompat.Channels.TYPE_PREVIEW }
            TableCount(preview) to TableCount(count - preview)
        }
        ListingsCheck(context.mayReadTvListings(), watchNext.first, channels.first, channels.second)
    }

    /**
     * Counts the rows of [uri], and gives the count and the values of [column] (none when it is null: only the count
     * is needed) to [counts]. No selection and no sort order: Android 9 refuses both from an app that cannot read
     * every app's data (ACCESS_ALL_EPG_DATA, for system apps only). A refusal, or no such table, gives the same
     * [TableCount] twice.
     */
    private fun query(
        uri: Uri,
        column: String?,
        counts: (Int, List<String>) -> Pair<TableCount, TableCount>
    ): Pair<TableCount, TableCount> = try {
        context.contentResolver.query(uri, arrayOf(column ?: BaseColumns._ID), null, null, null)?.use { c ->
            val values = if (column == null) emptyList() else buildList { while (c.moveToNext()) add(c.getString(0).orEmpty()) }
            counts(c.count, values)
        } ?: (TableCount(null) to TableCount(null))
    } catch (e: SecurityException) {
        refused(e)
    } catch (e: IllegalArgumentException) {
        refused(e) // a TV whose provider refuses the query
    }

    private fun refused(e: Exception) = TableCount(null, e.message ?: e.javaClass.simpleName).let { it to it }

    companion object {
        const val PERMISSION = "android.permission.READ_TV_LISTINGS"

        /** The command that gives the permission, for TVs that do not ask. */
        fun adbGrant(packageName: String) = "adb shell pm grant $packageName $PERMISSION"
    }
}

/** Whether the launcher may read other apps' Watch Next items and channels (the READ_TV_LISTINGS permission). */
fun Context.mayReadTvListings(): Boolean = checkSelfPermission(TvListings.PERMISSION) == PackageManager.PERMISSION_GRANTED
