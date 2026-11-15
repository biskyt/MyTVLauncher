package com.biskyt.mytvlauncher.core.speaker

import com.biskyt.mytvlauncher.core.ui.cycle

/**
 * The delay before the speaker wake sound (the owner, 2026-11-14): how long after the TV wakes the launcher waits, so
 * that the HDMI or ARC sound output is ready. Settings offers these, in seconds.
 */
object WakeDelay {
    val CHOICES = listOf(0, 1, 2, 3, 5, 10)
    const val DEFAULT = 2

    /**
     * The choice [step] places from [seconds] (Left -1, Right or OK 1), round from the last to the first and back. A
     * saved value that is not a choice steps from the nearest choice below it.
     */
    fun step(seconds: Int, step: Int): Int = CHOICES.cycle(CHOICES.lastOrNull { it <= seconds } ?: CHOICES.first(), step)
}
