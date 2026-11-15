package com.biskyt.mytvlauncher.core.ui

/**
 * The value [step] places from [current] in this list, round from the end to the start and back (as MyTVPlayer's
 * `cycle`): Left and Right on a setting shown as "◀ value ▶". A [current] not in the list steps from the first.
 */
fun <T> List<T>.cycle(current: T, step: Int = 1): T = this[Math.floorMod(indexOf(current).coerceAtLeast(0) + step, size)]
