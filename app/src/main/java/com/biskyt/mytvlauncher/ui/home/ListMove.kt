package com.biskyt.mytvlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.apps.AppOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Move mode for a list of items by key: the apps on Home's apps row and in All apps, and Home's rows in Settings.
 * [start] lifts an item, [step] moves it, and [end] (OK or Back) saves the order with [save], in the app's scope so that
 * leaving the screen does not lose it. After each step the item stays in view ([keepInView], the owner, 2026-11-14).
 */
class ListMove internal constructor(
    private val scope: CoroutineScope,
    private val appScope: CoroutineScope,
    private val save: suspend (List<String>) -> Unit,
    private val keepInView: suspend (key: String, index: Int) -> Unit
) {
    /** The key of the item being moved, or null. */
    var moving by mutableStateOf<String?>(null)
        private set

    private var order by mutableStateOf(emptyList<String>())

    /** A moved order being saved: shown until the saved list comes back, so that the items do not flick back meanwhile. */
    internal var saving by mutableStateOf(false)

    fun start(keys: List<String>, key: String) {
        order = keys
        moving = key
    }

    /** Moves the item [step] places. [holdScroll] first keeps the list where it is: a keyed list would follow the item. */
    fun step(key: String, step: Int, holdScroll: () -> Unit = {}) {
        holdScroll()
        order = AppOrder.move(order, key, step)
        scope.launch {
            // Laid out in its new place by the second frame.
            repeat(2) { withFrameNanos {} }
            keepInView(key, order.indexOf(key).coerceAtLeast(0))
        }
    }

    fun end() {
        val moved = order
        moving = null
        saving = true
        appScope.launch { save(moved) }
    }

    /** The items as shown: in the moved order while moving and saving, else [items]. */
    fun <T> shown(items: List<T>, byKey: Map<String, T>): List<T> = if (moving != null || saving) order.mapNotNull { byKey[it] } else items
}

/** A [ListMove] for [items] (the saved list: a new one ends the hold after a save), with Back ending the move. */
@Composable
fun rememberListMove(items: Any?, save: suspend (List<String>) -> Unit, keepInView: suspend (key: String, index: Int) -> Unit): ListMove {
    val scope = rememberCoroutineScope()
    val appScope = LocalContext.current.container.appScope
    val move = remember { ListMove(scope, appScope, save, keepInView) }
    LaunchedEffect(items) { move.saving = false }
    BackHandler(enabled = move.moving != null) { move.end() }
    return move
}
