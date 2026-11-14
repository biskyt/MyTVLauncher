package com.biskyt.mytvlauncher.core.channels

import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem

/**
 * An app's home screen channel (a preview channel): its [items] in the app's order. The items use Watch Next's type,
 * with the app's weight in `lastEngagementMs`.
 */
data class AppChannel(val id: Long, val packageName: String, val name: String, val items: List<WatchNextItem>)

/** The rules of the app channel rows. */
object Channels {
    /** The most items a channel row shows. */
    const val ITEMS = 30

    /** The rows Home shows: each channel the user did not turn off ([hiddenIds]) that has items, in the TV's order. */
    fun select(channels: List<AppChannel>, hiddenIds: Set<Long>): List<AppChannel> =
        channels.filter { it.id !in hiddenIds && it.items.isNotEmpty() }

    /** A channel's items, highest weight first (the app's own order), at most [ITEMS]. */
    fun order(items: List<WatchNextItem>): List<WatchNextItem> =
        items.sortedByDescending { it.lastEngagementMs }.distinctBy { it.key }.take(ITEMS)
}
