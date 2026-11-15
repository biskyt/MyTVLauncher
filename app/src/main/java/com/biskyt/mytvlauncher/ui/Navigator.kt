package com.biskyt.mytvlauncher.ui

import androidx.compose.runtime.mutableStateListOf

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

    /** Home was pressed twice: the recent apps over Home. */
    fun recents() {
        home()
        open(Screen.RECENTS)
    }
}
