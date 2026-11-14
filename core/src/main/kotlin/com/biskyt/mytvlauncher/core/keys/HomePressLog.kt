package com.biskyt.mytvlauncher.core.keys

/**
 * One Home press that reached the launcher: when (in ms, from the device's uptime clock), the gap since the last, and
 * whether the launcher was in front already ([inFront]) or another app was.
 */
data class HomePress(val atMs: Long, val gapMs: Long?, val inFront: Boolean, val endsDouble: Boolean = false)

/**
 * The Home presses that reached the launcher, newest first, for the Home key test in Settings. Android does not give
 * the Home key to apps. A launcher only gets "go home" for a short press. This log shows the owner what a held press
 * and a double press send on each TV, before the launcher acts on them (see ROADMAP.md).
 */
object HomePressLog {
    const val KEEP = 20

    /**
     * Two Home presses this close make a double press (the owner's test, 2026-11-14: 150 to 350 ms on the Philips TV,
     * longer than Android's usual 300 ms double tap).
     */
    const val DOUBLE_PRESS_MS = 500L

    /** Whether the newest of [presses] (newest first) ends a double press. */
    fun isDouble(presses: List<HomePress>): Boolean = presses.firstOrNull()?.endsDouble == true

    /** [presses] (newest first) with a press at [atMs] added, keeping the newest [keep]. */
    fun add(presses: List<HomePress>, atMs: Long, inFront: Boolean, keep: Int = KEEP): List<HomePress> = presses.firstOrNull().let { last ->
        val gap = last?.let { atMs - it.atMs }
        // A press that ended a double press does not start the next one: a third quick press is a single press.
        val endsDouble = gap != null && gap <= DOUBLE_PRESS_MS && last.endsDouble.not()
        (listOf(HomePress(atMs, gap, inFront, endsDouble)) + presses).take(keep)
    }
}
