package com.biskyt.mytvlauncher.core.channels

import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem

/**
 * An app's home screen channel (a preview channel): its [items] in the app's order. The items use Watch Next's type,
 * with the app's weight in `lastEngagementMs`. [browsable]: the TV shows it on its own home screen. An app has one
 * channel shown at first, and the TV's own launcher shows its others only when the user adds them.
 */
data class AppChannel(
    val id: Long,
    val packageName: String,
    val name: String,
    val items: List<WatchNextItem>,
    val browsable: Boolean = true
)

/** The rules of the app channel rows. */
object Channels {
    /** The most items a channel row shows. */
    const val ITEMS = 30

    /**
     * Whether Home shows [channel]: the user turned it on ([onIds]), or the TV shows it and the user did not turn it
     * off ([offIds]).
     */
    fun shown(channel: AppChannel, offIds: Set<Long>, onIds: Set<Long>): Boolean =
        channel.id in onIds || (channel.browsable && channel.id !in offIds)

    /** The rows Home shows: each channel that is [shown] and has items, in the TV's order. */
    fun select(channels: List<AppChannel>, offIds: Set<Long>, onIds: Set<Long> = emptySet()): List<AppChannel> =
        channels.filter { shown(it, offIds, onIds) && it.items.isNotEmpty() }

    /** A channel's items, highest weight first (the app's own order), at most [ITEMS]. */
    fun order(items: List<WatchNextItem>): List<WatchNextItem> =
        items.sortedByDescending { it.lastEngagementMs }.distinctBy { it.key }.take(ITEMS)
}
