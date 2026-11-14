package com.biskyt.mytvlauncher.data

import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract.Channels as TvChannels
import android.media.tv.TvContract.PreviewPrograms
import android.provider.BaseColumns
import com.biskyt.mytvlauncher.core.channels.AppChannel
import com.biskyt.mytvlauncher.core.channels.Channels
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.db.ChannelItemDao
import com.biskyt.mytvlauncher.data.db.ChannelItemRow
import com.biskyt.mytvlauncher.data.db.HiddenDao
import com.biskyt.mytvlauncher.data.db.HiddenKind
import com.biskyt.mytvlauncher.data.db.HiddenRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

/** Where the apps' home screen channels come from: the TV provider, or a fake one in tests. */
interface ChannelSource : ProviderSource {
    /** Every browsable preview channel with its browsable items, in the TV's order. Called in the background only. */
    fun read(): List<AppChannel>
}

/** A channel in Settings › Home rows: its name, its app's name, and whether its row is on Home. */
data class ChannelSwitch(val id: Long, val name: String, val app: String, val on: Boolean)

/**
 * The apps' channel rows (the owner, 2026-11-14), below the apps. Read as Continue watching is (see
 * [ProviderFollower]): Home draws the saved rows at once; with the rows off, nothing is read or watched.
 */
class ChannelRepository(
    private val dao: ChannelItemDao,
    private val hidden: HiddenDao,
    private val source: ChannelSource,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
    reads: Mutex
) {
    private val follower = ProviderFollower(source, settings.channelsShown, scope, reads, clear = { dao.deleteAll() }) {
        val rows = source.read().toRows()
        // Still on: the row may have been turned off while the read went.
        if (settings.channelsShown.first() && rows.toSet() != dao.all().toSet()) dao.replace(rows)
    }

    val allowed: StateFlow<Boolean> get() = follower.allowed

    private val saved: Flow<List<AppChannel>> = dao.observe().map { it.toChannels() }
    private val hiddenIds = hidden.observe(HiddenKind.CHANNEL)

    /** The rows Home shows, after the user's choices. */
    val channels: Flow<List<AppChannel>> = combine(saved, hiddenIds) { channels, off ->
        Channels.select(channels, off.mapNotNull { it.toLongOrNull() }.toSet())
    }

    /** Settings › Home rows: each channel, with its app's name (from [labels]), in the TV's order. */
    fun switches(labels: Flow<Map<String, String>>): Flow<List<ChannelSwitch>> = combine(saved, hiddenIds, labels) { channels, off, names ->
        channels.map { ChannelSwitch(it.id, it.name, names[it.packageName] ?: it.packageName, it.id.toString() !in off) }
    }

    fun start() = follower.start()

    fun cameToFront() = follower.cameToFront()

    fun wentToBack() = follower.wentToBack()

    fun refresh() = follower.refresh()

    /** Reads now: for tests. */
    suspend fun read() = follower.readNow()

    suspend fun setChannelShown(id: Long, on: Boolean) {
        if (on) hidden.show(HiddenKind.CHANNEL, id.toString()) else hidden.hide(HiddenRow(HiddenKind.CHANNEL, id.toString()))
    }
}

/** A browsable preview channel's id, app and name; null for any other row (the TV's own channels, hidden ones). */
private fun Cursor.toPreviewChannel(): Triple<Long, String, String>? {
    if (string(TvChannels.COLUMN_TYPE) != TvChannels.TYPE_PREVIEW || long(TvChannels.COLUMN_BROWSABLE) == 0L) return null
    val id = long(BaseColumns._ID) ?: return null
    return Triple(id, string(TvChannels.COLUMN_PACKAGE_NAME).orEmpty(), string(TvChannels.COLUMN_DISPLAY_NAME).orEmpty())
}

private fun List<AppChannel>.toRows(): List<ChannelItemRow> = flatMapIndexed { c, channel ->
    Channels.order(channel.items).mapIndexed { i, item ->
        ChannelItemRow(
            "${channel.id}/${item.key}", channel.id, channel.name, c, item.packageName, item.title, item.subtitle,
            item.pictureUri, item.shape, item.progress, i, item.intentUri
        )
    }
}

private fun List<ChannelItemRow>.toChannels(): List<AppChannel> = groupBy { it.channelId }.values.map { rows ->
    val first = rows.first()
    AppChannel(
        first.channelId,
        first.packageName,
        first.channelName,
        rows.map { WatchNextItem(it.key, it.packageName, it.title, it.subtitle, it.pictureUri, it.shape, it.progress, 0, it.intentUri) }
    )
}

/** The TV provider's preview channels and programs (Android's own TvContract names). */
class ProviderChannels(private val context: Context) : ChannelSource {
    private val watcher = ProviderWatcher(context, TvChannels.CONTENT_URI, PreviewPrograms.CONTENT_URI)

    override fun allowed(): Boolean = context.mayReadTvListings()

    override fun watch(onChange: () -> Unit) = watcher.watch(onChange)

    override fun stopWatching() = watcher.stop()

    /**
     * No projection, selection or sort order, as for Watch Next (Android 9 refuses a selection or sort order): every
     * channel, and every program once, grouped here. The channels table also holds the TV's own broadcast channels,
     * so only preview channels are kept.
     */
    override fun read(): List<AppChannel> = try {
        val channels = context.contentResolver.query(TvChannels.CONTENT_URI, null, null, null, null)?.use { c ->
            buildList { while (c.moveToNext()) c.toPreviewChannel()?.let(::add) }
        }.orEmpty()
        val wanted = channels.mapTo(HashSet()) { it.first }
        val programs = context.contentResolver.query(PreviewPrograms.CONTENT_URI, null, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val channel = c.long(PreviewPrograms.COLUMN_CHANNEL_ID)?.takeIf { it in wanted } ?: continue
                    c.toItem(PreviewPrograms.COLUMN_WEIGHT)?.let { add(channel to it) }
                }
            }
        }.orEmpty().groupBy({ it.first }, { it.second })
        channels.map { (id, pkg, name) -> AppChannel(id, pkg, name, programs[id].orEmpty()) }
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }
}
