package com.biskyt.mytvlauncher.core.ui

/**
 * How bright Home's background is, in percent (the owner, 2026-11-15: darker than 1.1.0, and set in Settings): how
 * much of the selected card's picture shows through the dark background, and the glows' strength in step with it. 0
 * is a plain dark background. The glow around the card with focus does not change with it.
 */
object BackdropLevel {
    /** 1.1.0's brightness (its overlay was 72% dark): the glows' strength at this level is their 1.1.0 strength. */
    const val FIRST_LEVEL = 28f

    val CHOICES = (0..40 step 5).toList()
    const val DEFAULT = 20

    /**
     * The choice [step] places from [percent] (Left -1, Right or OK 1), going round: after 40 comes Off again. OK and a
     * tap only step forward, so without this, touch users could not lower it (as MyTVPlayer 1.36.0).
     */
    fun step(percent: Int, step: Int): Int {
        val i = CHOICES.indexOfLast { it <= percent }.coerceAtLeast(0)
        return CHOICES[(i + step).mod(CHOICES.size)]
    }

    /** How much of the dark background lies over the backdrop at [percent] (1: all of it, so none of the picture). */
    fun overlayAlpha(percent: Int): Float = 1f - percent / 100f

    /** The glows' strength at [percent], as a share of their 1.1.0 strength (1 at [FIRST_LEVEL]). */
    fun glowScale(percent: Int): Float = percent / FIRST_LEVEL

    /** A saved or restored value as a choice: one that is not a choice gives the default. */
    fun valid(percent: Int): Int = percent.takeIf { it in CHOICES } ?: DEFAULT
}
