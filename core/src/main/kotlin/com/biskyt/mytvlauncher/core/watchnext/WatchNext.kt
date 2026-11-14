package com.biskyt.mytvlauncher.core.watchnext

/** The shape of a card in Continue watching: a 2:3 poster, or a wide 16:9 picture (the owner, 2026-11-14). */
enum class CardShape { POSTER, WIDE }

/**
 * One of another app's Watch Next items, as the launcher keeps it. [key] stays the same when the app writes the item
 * again (see [WatchNext.key]). [progress] is from 0 to 1, or null when the app gives no position.
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
    val intentUri: String?
)

/** The rules of the Continue watching row. */
object WatchNext {
    /** The most items the row shows (the owner, 2026-11-14). */
    const val LIMIT = 20

    // TvContractCompat's aspect ratio values (TvContract.PreviewPrograms.ASPECT_RATIO_*), kept here so core has no Android.
    private const val ASPECT_RATIO_2_3 = 4
    private const val ASPECT_RATIO_MOVIE_POSTER = 5

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
     * The picture and the card's shape. A poster when the app gives one in a tall ratio (2:3 or a film poster),
     * else the poster or thumbnail as a wide card.
     */
    fun picture(posterUri: String?, posterRatio: Int?, thumbnailUri: String?): Pair<String?, CardShape> = when {
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
     * An item's key: its app and the app's own id for it, else its row id. An app often deletes its item and writes it
     * again (a new row id), and a hidden item must stay hidden then.
     */
    fun key(packageName: String, internalProviderId: String?, rowId: Long): String =
        "$packageName/${internalProviderId?.takeIf { it.isNotBlank() } ?: "#$rowId"}"
}
