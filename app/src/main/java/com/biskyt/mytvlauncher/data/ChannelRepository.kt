package com.biskyt.mytvlauncher.data

import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract.Channels as TvChannels
import android.media.tv.TvContract.PreviewPrograms
import android.os.Build
import android.provider.BaseColumns
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

/** Where the apps' home screen channels come from: the TV provider, or a fake one in tests. */
interface ChannelSource : ProviderSource {
    /** Every preview channel with its browsable items, in the TV's order. Called in the background only. */
    fun read(): List<AppChannel>
}

/** A channel in Settings › Home rows: its name, and whether its row is on Home. */
data class ChannelSwitch(val id: Long, val name: String, val on: Boolean)

/** An app's channels in Settings › Home rows: many apps have several (the owner, 2026-11-14). */
data class AppChannels(val packageName: String, val app: String, val channels: List<ChannelSwitch>)

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
    /** The row's setting, off where the TV has no such rows (see hasTvRows). */
    val shown = settings.channelsShown.onlyWithTvRows()

    private val follower = ProviderFollower(source, shown, scope, reads, clear = { dao.deleteAll() }) {
        val rows = source.read().toRows()
        // Still on: the row may have been turned off while the read went.
        if (shown.first() && rows.toSet() != dao.all().toSet()) dao.replace(rows)
    }

    val allowed: StateFlow<Boolean> get() = follower.allowed

    private val saved: Flow<List<AppChannel>> = dao.observe().map { it.toChannels() }

    /** The user's choices: the channels turned off, and those turned on (one query for both). */
    private val choices: Flow<ChannelChoices> = hidden.observeKinds(listOf(HiddenKind.CHANNEL, HiddenKind.CHANNEL_ON))
        .map { rows -> rows.partition { it.kind == HiddenKind.CHANNEL }.let { (off, on) -> ChannelChoices(off.ids(), on.ids()) } }
        .distinctUntilChanged()

    /** The rows Home shows, after the user's choices. */
    val channels: Flow<List<AppChannel>> = combine(saved, choices) { channels, c -> Channels.select(channels, c.off, c.on) }

    /**
     * Settings › Home rows: each app's channels, by the app's name (from [labels]), A to Z, and each app's channels in
     * the TV's order.
     */
    fun switches(labels: Flow<Map<String, String>>): Flow<List<AppChannels>> = combine(saved, choices, labels) { channels, c, names ->
        channels.groupBy { it.packageName }
            .map { (pkg, list) ->
                AppChannels(pkg, names[pkg] ?: pkg, list.map { ChannelSwitch(it.id, it.name, Channels.shown(it, c.off, c.on)) })
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.app })
    }

    fun start() = follower.start()

    fun cameToFront() = follower.cameToFront()

    fun wentToBack() = follower.wentToBack()

    fun refresh() = follower.refresh()

    /** Reads now: for tests. */
    suspend fun read() = follower.readNow()

    /** Turns a channel's row on or off. Each choice is saved, so it holds whether the TV shows the channel or not. */
    suspend fun setChannelShown(id: Long, on: Boolean) {
        val key = id.toString()
        if (on) {
            hidden.show(HiddenKind.CHANNEL, key)
            hidden.hide(HiddenRow(HiddenKind.CHANNEL_ON, key))
        } else {
            hidden.show(HiddenKind.CHANNEL_ON, key)
            hidden.hide(HiddenRow(HiddenKind.CHANNEL, key))
        }
    }
}

private data class ChannelChoices(val off: Set<Long>, val on: Set<Long>)

private fun List<HiddenRow>.ids() = mapNotNullTo(HashSet()) { it.key.toLongOrNull() }

/**
 * A preview channel with no items yet; null for any other row (the TV's own broadcast channels). Channels that the TV
 * does not show are read too (the owner, 2026-11-14: apps such as Plex have several, and only one shows at first).
 */
private fun Cursor.toPreviewChannel(): AppChannel? {
    if (string(TvChannels.COLUMN_TYPE) != TvChannels.TYPE_PREVIEW) return null
    val id = long(BaseColumns._ID) ?: return null
    return AppChannel(
        id,
        string(TvChannels.COLUMN_PACKAGE_NAME).orEmpty(),
        string(TvChannels.COLUMN_DISPLAY_NAME).orEmpty(),
        emptyList(),
        browsable = long(TvChannels.COLUMN_BROWSABLE) != 0L
    )
}

private fun List<AppChannel>.toRows(): List<ChannelItemRow> = flatMapIndexed { c, channel ->
    Channels.order(channel.items).mapIndexed { i, item ->
        ChannelItemRow(
            "${channel.id}/${item.key}", channel.id, channel.name, c, item.packageName, item.title, item.subtitle,
            item.pictureUri, item.shape, item.progress, i, item.intentUri, channel.browsable
        )
    }
}

private fun List<ChannelItemRow>.toChannels(): List<AppChannel> = groupBy { it.channelId }.values.map { rows ->
    val first = rows.first()
    AppChannel(
        first.channelId,
        first.packageName,
        first.channelName,
        rows.map { WatchNextItem(it.key, it.packageName, it.title, it.subtitle, it.pictureUri, it.shape, it.progress, 0, it.intentUri) },
        first.channelBrowsable
    )
}

/** The TV provider's preview channels and programs, on Android 8 and later (Android's own TvContract names). */
@RequiresApi(Build.VERSION_CODES.O)
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
        val wanted = channels.mapTo(HashSet()) { it.id }
        val programs = context.contentResolver.query(PreviewPrograms.CONTENT_URI, null, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val channel = c.long(PreviewPrograms.COLUMN_CHANNEL_ID)?.takeIf { it in wanted } ?: continue
                    c.toItem(PreviewPrograms.COLUMN_WEIGHT)?.let { add(channel to it) }
                }
            }
        }.orEmpty().groupBy({ it.first }, { it.second })
        channels.map { it.copy(items = programs[it.id].orEmpty()) }
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }
}
