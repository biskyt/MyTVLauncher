package com.biskyt.mytvlauncher.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Runs [work] in the background when asked, one run at a time, never on the main thread. A request made while a run
 * goes is taken once it ends, not run alongside it. Requests that come together wait [settleMs] and make one run
 * (an app update sends removed, added and changed; a Watch Next change sends several notices). Every Refresher that
 * shares a [lock] runs one at a time: the launcher's reads of the installed apps and of the TV provider share one.
 */
// A TV's provider or package manager can throw anything: a failed run is logged, and the next request runs.
@Suppress("TooGenericExceptionCaught")
class Refresher(
    scope: CoroutineScope,
    private val settleMs: Long,
    private val lock: Mutex,
    private val work: suspend () -> Unit
) {
    private val requests = Channel<Long>(Channel.CONFLATED)

    init {
        scope.launch(Dispatchers.IO) {
            requests.consumeEach { wait ->
                delay(wait)
                requests.tryReceive() // asked again while waiting: this run covers it
                // A TV's provider or package manager can fail in ways of its own: that run is lost, not the launcher,
                // and the next request runs as usual.
                try {
                    now()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "A background read failed", e)
                }
            }
        }
    }

    /** Asks for a run in the background: at once, or after [settleMs] for a change notice. */
    fun request(afterChange: Boolean = false) {
        requests.trySend(if (afterChange) settleMs else 0L)
    }

    /** Runs now, after any run that goes. */
    suspend fun now() = exclusive { work() }

    /** Runs [block] with no run going alongside it (a delete that a run must not write over). */
    suspend fun exclusive(block: suspend () -> Unit) = withContext(Dispatchers.IO) { lock.withLock { block() } }

    private companion object {
        const val TAG = "MyTVLauncher"
    }
}
