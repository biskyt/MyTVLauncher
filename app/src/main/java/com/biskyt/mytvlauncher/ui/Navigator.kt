package com.biskyt.mytvlauncher.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/** The launcher's screens. */
enum class Screen {
    HOME,
    SETTINGS,
    ACCENT,
    HOME_ROWS,
    ROW_ORDER,
    BACKUP,
    SPEAKER_WAKE,
    ALL_APPS,
    RECENTS,
}

/** The screens open, Home at the bottom. */
class Navigator {
    private val stack = mutableStateListOf(Screen.HOME)

    val top: Screen get() = stack.last()

    fun open(screen: Screen) {
        stack.add(screen)
    }

    /** Closes the top screen. Back does not leave Home. */
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Home was pressed: back to Home. */
    fun home() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /**
     * When Home was last reset, in ms of the device's uptime (0: never): Home pressed once while Home was in front (the
     * owner, 2026-11-17). Home then shows its start again (see LocalHomeReset). A time, not a count: a list's saved
     * state outlives the activity, and with it this navigator, and a time is still later than the one it saved.
     */
    var homeReset by mutableLongStateOf(0L)
        private set

    /** Home was pressed once while Home was in front: Home goes back to its start. */
    fun resetHome(atMs: Long = SystemClock.uptimeMillis()) {
        homeReset = atMs
    }

    /** Home was pressed twice: the recent apps over Home. */
    fun recents() {
        home()
        open(Screen.RECENTS)
    }
}
