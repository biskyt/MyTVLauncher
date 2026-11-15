package com.biskyt.mytvlauncher.core.notifications

/**
 * A system notification as the launcher shows it (the owner, 2026-11-15): its [key] (Android's, to open or dismiss
 * it), its app, its title and text, when it was posted, whether it is a group's summary (Android shows a group of
 * notices from one app under one summary), and whether the user can dismiss it.
 */
data class TvNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    val summary: Boolean = false,
    val clearable: Boolean = true
)

/** Which notifications the launcher shows, and in what order. */
object TvNotifications {
    /**
     * The notifications to show, newest first: not the launcher's own ([own]), not a group's summary (its notices are
     * shown one by one), not one that cannot be dismissed (an app's "is running" notice or a player: the count would
     * never go down), and not one with neither a title nor a text (nothing to read).
     */
    fun shown(all: List<TvNotification>, own: String): List<TvNotification> = all
        .filter { it.packageName != own && !it.summary && it.clearable && (!it.title.isNullOrBlank() || !it.text.isNullOrBlank()) }
        .sortedByDescending { it.postedAt }
}
