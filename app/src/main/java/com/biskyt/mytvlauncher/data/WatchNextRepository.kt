package com.biskyt.mytvlauncher.data

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract.WatchNextPrograms
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.biskyt.mytvlauncher.core.watchnext.WatchNext
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.db.HiddenDao
import com.biskyt.mytvlauncher.data.db.HiddenKind
import com.biskyt.mytvlauncher.data.db.HiddenRow
import com.biskyt.mytvlauncher.data.db.WatchNextDao
import com.biskyt.mytvlauncher.data.db.WatchNextRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

/** Where other apps' Watch Next items come from: the TV provider, or a fake one in tests. */
interface WatchNextSource : ProviderSource {
    /** Every item now. Called in the background only. */
    fun read(): List<WatchNextItem>
}

/** An app in Settings › Home rows: its name, and whether its items are in the row. */
data class AppSwitch(val packageName: String, val label: String, val on: Boolean)

/**
 * Continue watching. Home reads the saved items ([items]), so its first frame needs no read of the TV provider. A
 * read runs in the background, one at a time: when Home comes to the front after an app changed its items (or when
 * the launcher cannot follow changes), when the permission is given, and while Home is in front, after an app changes
 * its items (a short wait gathers its notices into one read). It writes the items only when they changed. With the row
 * off (Settings › Home rows), nothing is read or watched, and the saved items are deleted.
 */
class WatchNextRepository(
    private val dao: WatchNextDao,
    private val hidden: HiddenDao,
    private val source: WatchNextSource,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
    reads: Mutex
) {
    private val follower = ProviderFollower(source, settings.watchNextShown, scope, reads, clear = { dao.deleteAll() }) {
        // An app can write two rows with one id of its own (an episode both to carry on with and next): one is kept.
        val rows = source.read().map { it.toRow() }.distinctBy { it.key }
        // Still on: the row may have been turned off while the read went.
        if (settings.watchNextShown.first() && rows.toSet() != dao.all().toSet()) dao.replace(rows)
    }

    /** Whether the launcher may read the items (see [ProviderFollower.allowed]). */
    val allowed: StateFlow<Boolean> get() = follower.allowed

    private val rows = dao.observe()
    private val hiddenApps = hidden.observe(HiddenKind.WATCH_NEXT_APP)
    private val hiddenKeys = hidden.observe(HiddenKind.WATCH_NEXT_ITEM)

    /**
     * The items the row shows, after the user's choices: the newest [WatchNext.LIMIT]. Home also leaves the row out
     * while it is off; the saved items are deleted then too.
     */
    val items: Flow<List<WatchNextItem>> = combine(rows, hiddenApps, hiddenKeys) { rows, apps, keys ->
        WatchNext.select(rows.map { it.toItem() }, apps.toSet(), keys.toSet())
    }

    /**
     * Settings › Home rows: each app that has items, or that the user took out of the row, by name (from [labels], the
     * installed apps' names), A to Z.
     */
    fun appSwitches(labels: Flow<Map<String, String>>): Flow<List<AppSwitch>> = combine(rows, hiddenApps, labels) { rows, out, names ->
        val off = out.toSet()
        (rows.map { it.packageName } + out).distinct()
            .map { AppSwitch(it, names[it] ?: it, it !in off) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** How many items the user hid one by one. */
    val hiddenItems: Flow<Int> = hiddenKeys.map { it.size }

    /** See [ProviderFollower]: following the row's setting, Home's comings and goings, and the permission. */
    fun start() = follower.start()

    fun cameToFront() = follower.cameToFront()

    fun wentToBack() = follower.wentToBack()

    fun refresh() = follower.refresh()

    /** Reads now: for tests. */
    suspend fun read() = follower.readNow()

    suspend fun hideItem(key: String) = hidden.hide(HiddenRow(HiddenKind.WATCH_NEXT_ITEM, key))

    suspend fun showAllItems() = hidden.showAll(HiddenKind.WATCH_NEXT_ITEM)

    suspend fun setAppShown(packageName: String, on: Boolean) {
        if (on) hidden.show(HiddenKind.WATCH_NEXT_APP, packageName) else hidden.hide(HiddenRow(HiddenKind.WATCH_NEXT_APP, packageName))
    }
}

private fun WatchNextRow.toItem() =
    WatchNextItem(key, packageName, title, subtitle, pictureUri, shape, progress, lastEngagementMs, intentUri)

private fun WatchNextItem.toRow() =
    WatchNextRow(key, packageName, title, subtitle, pictureUri, shape, progress, lastEngagementMs, intentUri)

/** The TV provider's Watch Next table (Android's own TvContract names: some of tvprovider's are restricted). */
class ProviderWatchNext(private val context: Context) : WatchNextSource {
    private val watcher = ProviderWatcher(context, WatchNextPrograms.CONTENT_URI)

    override fun allowed(): Boolean = context.mayReadTvListings()

    /**
     * Every browsable row. No projection, selection or sort order: Android 9 refuses a selection or sort order from an
     * app without ACCESS_ALL_EPG_DATA, and a column a TV lacks reads as null. Sorted and cut in core.
     */
    override fun read(): List<WatchNextItem> = try {
        context.contentResolver.query(WatchNextPrograms.CONTENT_URI, null, null, null, null)?.use { c ->
            buildList { while (c.moveToNext()) c.toItem()?.let(::add) }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    override fun watch(onChange: () -> Unit) = watcher.watch(onChange)

    override fun stopWatching() = watcher.stop()
}

/** Follows changes to [uris] of the TV provider, on a binder thread (not the main one). */
class ProviderWatcher(private val context: Context, private vararg val uris: Uri) {
    private var observer: ContentObserver? = null

    fun watch(onChange: () -> Unit): Boolean {
        if (observer != null) return true
        val o = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) = onChange()
        }
        return try {
            uris.forEach { context.contentResolver.registerContentObserver(it, true, o) }
            observer = o
            true
        } catch (_: SecurityException) {
            context.contentResolver.unregisterContentObserver(o)
            false
        }
    }

    fun stop() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
    }
}

internal fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let(::getStringOrNull)
internal fun Cursor.long(column: String): Long? = getColumnIndex(column).takeIf { it >= 0 }?.let(::getLongOrNull)

/**
 * A Watch Next or preview program row as core's item; null for a row the TV does not show (not browsable) or with no
 * title. The two tables share these columns. [sortColumn] fills `lastEngagementMs`, which orders the items (Watch
 * Next: when last watched; a channel: the app's weight).
 */
internal fun Cursor.toItem(sortColumn: String = WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS): WatchNextItem? {
    if (long(WatchNextPrograms.COLUMN_BROWSABLE) == 0L) return null
    val packageName = string(WatchNextPrograms.COLUMN_PACKAGE_NAME) ?: return null
    val title = string(WatchNextPrograms.COLUMN_TITLE)?.takeIf { it.isNotBlank() } ?: return null
    val (picture, shape) = WatchNext.picture(
        string(WatchNextPrograms.COLUMN_POSTER_ART_URI),
        long(WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO)?.toInt(),
        string(WatchNextPrograms.COLUMN_THUMBNAIL_URI)
    )
    return WatchNextItem(
        key = WatchNext.key(packageName, string(WatchNextPrograms.COLUMN_INTERNAL_PROVIDER_ID), long(BaseColumns._ID) ?: 0),
        packageName = packageName,
        title = title,
        subtitle = string(WatchNextPrograms.COLUMN_EPISODE_TITLE),
        pictureUri = picture,
        shape = shape,
        progress = WatchNext.progress(
            long(WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS),
            long(WatchNextPrograms.COLUMN_DURATION_MILLIS)
        ),
        lastEngagementMs = long(sortColumn) ?: 0,
        intentUri = string(WatchNextPrograms.COLUMN_INTENT_URI)
    )
}
