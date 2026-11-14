package com.biskyt.mytvlauncher.ui

import androidx.compose.runtime.mutableStateListOf

/** The launcher's screens. [keptOnHome]: a Home press does not close it (see [Navigator.home]). */
enum class Screen(val keptOnHome: Boolean = false) {
    HOME,
    SETTINGS,
    ACCENT,
    HOME_ROWS,
    ROW_ORDER,
    BACKUP,
    ALL_APPS,
    RECENTS,

    /** The Home key test shows what each press sends. */
    CHECK(keptOnHome = true)
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

    /** Home was pressed: back to Home, unless the top screen is [Screen.keptOnHome]. */
    fun home() {
        if (top.keptOnHome) return
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Home was pressed twice: the recent apps over Home, unless the top screen is [Screen.keptOnHome]. */
    fun recents() {
        if (top.keptOnHome) return
        home()
        open(Screen.RECENTS)
    }
}
