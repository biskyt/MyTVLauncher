package com.biskyt.mytvlauncher.core.speaker

/**
 * The delay before the speaker wake sound (the owner, 2026-11-14): how long after the TV wakes the launcher waits, so
 * that the HDMI or ARC sound output is ready. Settings offers these, in seconds.
 */
object WakeDelay {
    val CHOICES = listOf(0, 1, 2, 3, 5, 10)
    const val DEFAULT = 2

    /** The choice after [seconds], back to the first after the last (OK on the setting steps through them). */
    fun next(seconds: Int): Int = CHOICES.firstOrNull { it > seconds } ?: CHOICES.first()
}
