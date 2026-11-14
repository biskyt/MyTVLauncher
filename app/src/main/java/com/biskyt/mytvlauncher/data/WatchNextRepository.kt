package com.biskyt.mytvlauncher.data

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract.WatchNextPrograms
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Where other apps' Watch Next items come from: the TV provider, or a fake one in tests. */
interface WatchNextSource {
    /** Whether the launcher may read them (the READ_TV_LISTINGS permission). */
    fun allowed(): Boolean

    /** Every item now. Called in the background only. */
    fun read(): List<WatchNextItem>

    /** Calls [onChange] whenever an item changes, until [stopWatching]; false if it cannot follow changes. */
    fun watch(onChange: () -> Unit): Boolean

    fun stopWatching()
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
    private val scope: CoroutineScope
) {
    private val reads = Refresher(scope, SETTLE_MS) { readNow() }

    @Volatile private var shown = false

    /** Whether Home is in front: changes made meanwhile are read when it comes back (see [changed]). */
    @Volatile private var inFront = false

    /** An app changed its items while Home was not in front, or the launcher cannot follow changes. */
    private val changed = AtomicBoolean(true)

    private val allowedNow = MutableStateFlow(source.allowed())

    /** Whether the launcher may read the items: checked again at each refresh (a grant from adb or Android settings). */
    val allowed: StateFlow<Boolean> = allowedNow.asStateFlow()

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

    /** Starts following the row's setting: watching and reading while it is on. */
    fun start() {
        scope.launch {
            settings.watchNextShown.collect { on ->
                shown = on
                if (on) {
                    changed.set(!source.watch(::onChange))
                    reads.request()
                } else {
                    source.stopWatching()
                    // After any read that goes, so that it cannot write the items back.
                    reads.exclusive { dao.deleteAll() }
                }
            }
        }
    }

    /** An app changed its items: read now if Home is in front, else when it comes back. */
    private fun onChange() {
        if (inFront) reads.request(afterChange = true) else changed.set(true)
    }

    /** Home came to the front: read if anything changed meanwhile. */
    fun cameToFront() {
        inFront = true
        allowedNow.value = source.allowed()
        if (shown && changed.getAndSet(false)) reads.request()
    }

    fun wentToBack() {
        inFront = false
    }

    /** Asks for a read in the background (the permission was given). */
    fun refresh() {
        allowedNow.value = source.allowed()
        if (shown) reads.request()
    }

    /** Reads now: for tests. */
    suspend fun read() = reads.now()

    private suspend fun readNow() {
        if (!shown) return
        if (!source.allowed()) {
            // The permission was taken away: other apps' items are not kept either.
            dao.deleteAll()
            return
        }
        // An app can write two rows with one id of its own (an episode both to carry on with and next): one is kept.
        val rows = source.read().map { it.toRow() }.distinctBy { it.key }
        if (shown && rows.toSet() != dao.all().toSet()) dao.replace(rows)
    }

    suspend fun hideItem(key: String) = hidden.hide(HiddenRow(HiddenKind.WATCH_NEXT_ITEM, key))

    suspend fun showAllItems() = hidden.showAll(HiddenKind.WATCH_NEXT_ITEM)

    suspend fun setAppShown(packageName: String, on: Boolean) {
        if (on) hidden.show(HiddenKind.WATCH_NEXT_APP, packageName) else hidden.hide(HiddenRow(HiddenKind.WATCH_NEXT_APP, packageName))
    }

    private companion object {
        const val SETTLE_MS = 500L
    }
}

private fun WatchNextRow.toItem() =
    WatchNextItem(key, packageName, title, subtitle, pictureUri, shape, progress, lastEngagementMs, intentUri)

private fun WatchNextItem.toRow() =
    WatchNextRow(key, packageName, title, subtitle, pictureUri, shape, progress, lastEngagementMs, intentUri)

/** The TV provider's Watch Next table (Android's own TvContract names: some of tvprovider's are restricted). */
class ProviderWatchNext(private val context: Context) : WatchNextSource {
    private var observer: ContentObserver? = null

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

    override fun watch(onChange: () -> Unit): Boolean {
        if (observer != null) return true
        // No handler: notices come on a binder thread, not the main one.
        val o = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) = onChange()
        }
        return try {
            context.contentResolver.registerContentObserver(WatchNextPrograms.CONTENT_URI, true, o)
            observer = o
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun stopWatching() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
    }
}

private fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let(::getStringOrNull)
private fun Cursor.long(column: String): Long? = getColumnIndex(column).takeIf { it >= 0 }?.let(::getLongOrNull)

/** A Watch Next row as core's item; null for a row the TV does not show (not browsable) or with no title. */
private fun Cursor.toItem(): WatchNextItem? {
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
        lastEngagementMs = long(WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS) ?: 0,
        intentUri = string(WatchNextPrograms.COLUMN_INTENT_URI)
    )
}
