package com.biskyt.mytvlauncher.core.rows

/**
 * The order of the rows on Home, which the user chooses (the owner, 2026-11-14): Continue watching, the apps, and each
 * app channel's row, by key.
 */
object RowOrder {
    const val WATCH_NEXT = "watch_next"
    const val APPS = "apps"

    fun channel(id: Long) = "channel:$id"

    /**
     * [rows] (in Home's usual order: Continue watching, the apps, then the channels) in the [saved] order. A row that
     * is not in [saved] (a new channel) keeps its usual order, after the saved rows.
     */
    fun order(rows: List<String>, saved: List<String>): List<String> {
        val rank = saved.withIndex().associate { (i, key) -> key to i }
        return rows.withIndex().sortedWith(compareBy({ rank[it.value] ?: Int.MAX_VALUE }, { it.index })).map { it.value }
    }

    /**
     * The order to save after the user moved the rows shown in Settings into [moved]. Rows saved before but not shown
     * now (a channel turned off) are kept after them, so that they have a place when they come back.
     */
    fun save(moved: List<String>, saved: List<String>): List<String> = moved + saved.filter { it !in moved }
}
