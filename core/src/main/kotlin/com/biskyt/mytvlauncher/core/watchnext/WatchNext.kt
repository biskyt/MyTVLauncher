package com.biskyt.mytvlauncher.core.watchnext

import com.biskyt.mytvlauncher.core.ui.cycle

/**
 * The shape of a card: a 2:3 poster, or a wide 16:9 picture (the owner, 2026-11-14). A [LOGO] card is wide, with a TV
 * channel's logo in it, on a dark see-through card.
 */
enum class CardShape { POSTER, WIDE, LOGO }

/**
 * One of another app's Watch Next items, as the launcher keeps it. [key] stays the same when the app writes the item
 * again (see [WatchNext.key]). [progress] is from 0 to 1, or null when the app gives no position. [season] and
 * [episode] are the app's display numbers, and [leftMs] the time left to watch (see [WatchNext.left]).
 */
data class WatchNextItem(
    val key: String,
    val packageName: String,
    val title: String,
    val subtitle: String?,
    val pictureUri: String?,
    val shape: CardShape,
    val progress: Float?,
    val lastEngagementMs: Long,
    val intentUri: String?,
    val season: String? = null,
    val episode: String? = null,
    val leftMs: Long? = null
)

/** The rules of the Continue watching row. */
object WatchNext {
    /**
     * The most items the row shows, a choice in Settings › Home rows (the owner, 2026-11-25: 30 by default, from 10 to
     * 40; it was 20).
     */
    val LIMITS = (10..40 step 5).toList()
    const val LIMIT = 30

    /** The choice [step] places from [limit] (Left -1, Right or OK 1), going round, as WakeDelay.step does. */
    fun stepLimit(limit: Int, step: Int): Int = LIMITS.cycle(validLimit(limit), step)

    /** A saved or restored limit as a choice: one that is not a choice gives the default. */
    fun validLimit(limit: Int): Int = limit.takeIf { it in LIMITS } ?: LIMIT

    // TvContractCompat's aspect ratio values (TvContract.PreviewPrograms.ASPECT_RATIO_*), kept here so core has no Android.
    private const val ASPECT_RATIO_2_3 = 4
    private const val ASPECT_RATIO_MOVIE_POSTER = 5

    // TvContract.PreviewPrograms.TYPE_CHANNEL: the item is a TV channel, and its picture is the channel's logo.
    private const val TYPE_CHANNEL = 6

    /**
     * The items the row shows: not from an app the user took out ([hiddenApps]), not hidden ([hiddenItems], by key),
     * newest first, at most [limit].
     */
    fun select(items: List<WatchNextItem>, hiddenApps: Set<String>, hiddenItems: Set<String>, limit: Int = LIMIT): List<WatchNextItem> =
        items
            .filter { it.packageName !in hiddenApps && it.key !in hiddenItems }
            .sortedByDescending { it.lastEngagementMs }
            .take(limit)

    /**
     * The picture and the card's shape. A TV channel's ([type]) picture is its logo. Otherwise a poster when the app
     * gives one in a tall ratio (2:3 or a film poster), else the poster or thumbnail as a wide card.
     */
    fun picture(posterUri: String?, posterRatio: Int?, thumbnailUri: String?, type: Int? = null): Pair<String?, CardShape> = when {
        type == TYPE_CHANNEL -> (posterUri ?: thumbnailUri) to CardShape.LOGO
        posterUri != null && posterRatio.isTall() -> posterUri to CardShape.POSTER
        posterUri != null -> posterUri to CardShape.WIDE
        else -> thumbnailUri to CardShape.WIDE
    }

    private fun Int?.isTall() = this == ASPECT_RATIO_2_3 || this == ASPECT_RATIO_MOVIE_POSTER

    /** How far through the item is, from 0 to 1, or null when the app gives no position or duration. */
    fun progress(positionMs: Long?, durationMs: Long?): Float? {
        if (positionMs == null || durationMs == null || positionMs <= 0 || durationMs <= 0) return null
        return (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    /**
     * The time left to watch: the whole length when not started, else what is left after the position. Null when the
     * app gives no length, or nothing is left.
     */
    fun left(positionMs: Long?, durationMs: Long?): Long? {
        if (durationMs == null || durationMs <= 0) return null
        return (durationMs - (positionMs ?: 0).coerceAtLeast(0)).takeIf { it > 0 }
    }

    /**
     * An item's key: its app and the app's own id for it, else its row id. An app often deletes its item and writes it
     * again (a new row id), and a hidden item must stay hidden then.
     */
    fun key(packageName: String, internalProviderId: String?, rowId: Long): String =
        "$packageName/${internalProviderId?.takeIf { it.isNotBlank() } ?: "#$rowId"}"
}
