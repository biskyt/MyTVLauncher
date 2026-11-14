package com.biskyt.mytvlauncher.core.keys

/**
 * One Home press that reached the launcher: when (in ms, from the device's uptime clock), the gap since the last, and
 * whether the launcher was in front already ([inFront]) or another app was.
 */
data class HomePress(val atMs: Long, val gapMs: Long?, val inFront: Boolean)

/**
 * The Home presses that reached the launcher, newest first, for the Home key test in Settings. Android does not give
 * the Home key to apps. A launcher only gets "go home" for a short press. This log shows the owner what a held press
 * and a double press send on each TV, before the launcher acts on them (see ROADMAP.md).
 */
object HomePressLog {
    const val KEEP = 20

    /** [presses] (newest first) with a press at [atMs] added, keeping the newest [keep]. */
    fun add(presses: List<HomePress>, atMs: Long, inFront: Boolean, keep: Int = KEEP): List<HomePress> =
        (listOf(HomePress(atMs, presses.firstOrNull()?.let { atMs - it.atMs }, inFront)) + presses).take(keep)
}
