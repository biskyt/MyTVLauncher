package com.biskyt.mytvlauncher.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** Remote buttons that open a context menu; remotes label them Info, Options, Menu or "…". */
private val optionsKeys = setOf(Key.Info, Key.Menu, Key.TvContentsMenu, Key.TvMediaContextMenu)

fun Key.isOptions(): Boolean = this in optionsKeys

/**
 * Opens [onOptions] from the remote's Info/Options/Menu button, as an alternative to holding OK.
 */
fun Modifier.onOptionsKey(onOptions: () -> Unit): Modifier = onPreviewKeyEvent { e -> handlePress(e, e.key.isOptions(), onOptions) }

/** Consumes the whole press (down, repeats, up) when [matches], acting once on the first key-down. */
private fun handlePress(e: KeyEvent, matches: Boolean, action: () -> Unit): Boolean {
    if (!matches) return false
    if (e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0) action()
    return true
}
