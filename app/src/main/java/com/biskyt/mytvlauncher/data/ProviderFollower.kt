package com.biskyt.mytvlauncher.data

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/** A part of the TV provider that the launcher reads (Watch Next, app channels): the permission, and its changes. */
interface ProviderSource {
    /** Whether the launcher may read it (the READ_TV_LISTINGS permission). */
    fun allowed(): Boolean

    /** Calls [onChange] whenever it changes, until [stopWatching]; false if it cannot follow changes. */
    fun watch(onChange: () -> Unit): Boolean

    fun stopWatching()
}

/**
 * When the launcher reads a part of the TV provider into Room, for a Home row that a setting turns on or off ([shown]).
 * Reads run in the background, one at a time (see [Refresher]): when the row is turned on, when Home comes to the front
 * after a change (or when the launcher cannot follow changes), when the permission is given, and while Home is in
 * front, after a change (a short wait gathers its notices into one read). With the row off, nothing is read or
 * watched, and [clear] deletes the saved rows. Without the permission, [clear] runs too.
 */
class ProviderFollower(
    private val source: ProviderSource,
    private val shown: Flow<Boolean>,
    private val scope: CoroutineScope,
    reads: Mutex,
    private val clear: suspend () -> Unit,
    private val read: suspend () -> Unit
) {
    private val reads = Refresher(scope, SETTLE_MS, reads) { readIfShown() }

    /** The row's setting, as [start] last saw it: only to decide whether to ask for a read. */
    @Volatile private var on = false

    /** Whether Home is in front: changes made meanwhile are read when it comes back (see [changed]). */
    @Volatile private var inFront = false

    /** A change came while Home was not in front, or the launcher cannot follow changes. */
    private val changed = AtomicBoolean(true)

    private val allowedNow = MutableStateFlow(source.allowed())

    /** Whether the launcher may read: checked again at each return to Home and refresh (a grant from adb or settings). */
    val allowed: StateFlow<Boolean> = allowedNow.asStateFlow()

    /** Starts following the row's setting: watching and reading while it is on. */
    fun start() {
        scope.launch {
            shown.collect {
                on = it
                if (it) {
                    changed.set(!source.watch(::onChange))
                    reads.request()
                } else {
                    source.stopWatching()
                    // After any read that goes, so that it cannot write the rows back.
                    reads.exclusive { clear() }
                }
            }
        }
    }

    private fun onChange() {
        if (inFront) reads.request(afterChange = true) else changed.set(true)
    }

    /** Home came to the front: read if anything changed meanwhile, or if the permission was given meanwhile. */
    fun cameToFront() {
        inFront = true
        val given = checkAllowed()
        if (on && (changed.getAndSet(false) || given)) reads.request()
    }

    fun wentToBack() {
        inFront = false
    }

    /** Asks for a read in the background (the permission was given). */
    fun refresh() {
        checkAllowed()
        if (on) reads.request()
    }

    /** Checks the permission again: true if it was given since the last check. */
    private fun checkAllowed(): Boolean {
        val now = source.allowed()
        val before = allowedNow.getAndUpdate { now }
        return now && !before
    }

    /** Reads now, after any read that goes: for tests. */
    suspend fun readNow() = reads.now()

    /** Whether the row is on, by the saved setting itself (not [on], which [start]'s collector may not have set yet). */
    private suspend fun isShown() = shown.first()

    private suspend fun readIfShown() {
        if (!isShown()) return
        if (!source.allowed()) {
            // The permission was taken away: other apps' data is not kept either.
            clear()
            return
        }
        read()
    }

    private companion object {
        const val SETTLE_MS = 500L
    }
}
