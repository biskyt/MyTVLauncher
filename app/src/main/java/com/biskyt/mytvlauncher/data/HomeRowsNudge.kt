package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Asks MyTVPlayer and MyPlexPlayer to bring their rows on Home up to date (the owner, 2026-11-15): their Continue
 * watching items and their channels. Each app syncs what was watched elsewhere only while it runs, so after standby
 * the launcher's rows could be out of date. Android has no such request of its own, so this is a direct broadcast,
 * `<package>.action.REFRESH_HOME_ROWS` (as the TV guide's `<package>.action.TV_GUIDE`). The app syncs and writes its
 * items, and the rows follow them as they follow every change. Sent when the TV wakes and when Home comes to the
 * front, at most once in [MIN_GAP_MS], and only while Continue watching or the channel rows are on. It is sent
 * [waitMs] later: after a wake the network needs a few seconds, and at start-up Home draws first. An app that is not
 * installed, or does not answer it, gets nothing.
 */
class HomeRowsNudge(
    private val context: Context,
    private val rowsShown: Flow<Boolean>,
    private val scope: CoroutineScope,
    private val waitMs: Long = WAIT_MS,
    private val now: () -> Long = SystemClock::elapsedRealtime
) {
    private var last: Long? = null

    fun nudge() {
        scope.launch {
            delay(waitMs)
            if (!rowsShown.first() || !due()) return@launch
            // Also to an app in the stopped state (just installed, or stopped by the user), which would miss it.
            APPS.forEach { app ->
                context.sendBroadcast(Intent(action(app)).setPackage(app).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES))
            }
        }
    }

    /** Whether [MIN_GAP_MS] has passed since the last request, and if so, counts this one. */
    @Synchronized
    private fun due(): Boolean {
        val t = now()
        val before = last
        if (before != null && t - before < MIN_GAP_MS) return false
        last = t
        return true
    }

    companion object {
        /** The apps asked: MyTVPlayer, which syncs between the user's TVs, and MyPlexPlayer, with the Plex server. */
        val APPS = listOf("com.biskyt.mytvplayer", "com.biskyt.myplexplayer")

        const val MIN_GAP_MS = 5 * 60_000L
        const val WAIT_MS = 8_000L

        fun action(app: String) = "$app.action.REFRESH_HOME_ROWS"
    }
}
