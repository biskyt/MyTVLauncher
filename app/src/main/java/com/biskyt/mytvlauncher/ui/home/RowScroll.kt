package com.biskyt.mytvlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import com.biskyt.mytvlauncher.ui.common.settleFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * How the launcher's lists scroll to the item with focus: only as far as they must to show it, with [marginPx] around
 * it. On a TV, Compose otherwise keeps the item with focus about a third of the way across. A row then scrolled as the
 * remote moved along it, and again when the remote moved up or down onto it (the owner, 2026-11-14: "jarring").
 * With this, Up and Down land on the card under the one that had focus, and the row stays where it is.
 */
@OptIn(ExperimentalFoundationApi::class)
class KeepInViewSpec(private val marginPx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        keepInView(offset, offset + size, 0f, containerSize, marginPx)
}

/**
 * How far to scroll so that an item from [start] to [end] is inside the visible part, from [from] to [to], with
 * [margin] at each side: 0 if it is already. An item too large for the space goes to the start.
 */
internal fun keepInView(start: Float, end: Float, from: Float, to: Float, margin: Float): Float = when {
    end - start + 2 * margin >= to - from -> start - margin - from
    start - margin < from -> start - margin - from
    end + margin > to -> end + margin - to
    else -> 0f
}

/** Scrolls the row so that the item with [key] (the app being moved) stays in view, [marginPx] from each side. */
suspend fun LazyListState.keepInView(key: Any, index: Int, marginPx: Float) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key }
    if (item == null) return scrollToItem(index)
    scrollToShow(item.offset, item.offset + item.size, info.viewportStartOffset, info.viewportEndOffset, marginPx)
}

/** The same for a grid: the row of the item with [key] stays in view. */
suspend fun LazyGridState.keepInView(key: Any, index: Int, marginPx: Float) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key }
    if (item == null) return scrollToItem(index)
    scrollToShow(item.offset.y, item.offset.y + item.size.height, info.viewportStartOffset, info.viewportEndOffset, marginPx)
}

/** Scrolls by as much as [keepInView] says an item from [start] to [end] needs. */
private suspend fun ScrollableState.scrollToShow(start: Int, end: Int, from: Int, to: Int, margin: Float) {
    val distance = keepInView(start.toFloat(), end.toFloat(), from.toFloat(), to.toFloat(), margin)
    if (distance != 0f) scrollBy(distance)
}

/** [ROW_PAD] in pixels: how far from a list's sides the item with focus stays. */
@Composable
fun rememberRowPadPx(): Float = with(LocalDensity.current) { ROW_PAD.toPx() }

/**
 * Back on a row (the owner, 2026-11-14): from any item but the first, the row scrolls back to its start and its first
 * item takes focus. On the first item, Back does what it did before. Put [row] on the row and [item] on each item.
 */
class RowBack internal constructor(private val list: LazyListState, private val scope: CoroutineScope) {
    private val first = FocusRequester()

    /** The index of the row's item with focus, or -1 when the row has none. */
    private var focused by mutableIntStateOf(-1)

    /** Whether an item after the first has focus: read by Back, so that a move along the row recomposes nothing. */
    internal val pastFirst by derivedStateOf { focused > 0 }

    val row: Modifier = Modifier.staysInRow().onFocusChanged { if (!it.hasFocus) focused = -1 }

    fun item(index: Int, modifier: Modifier = Modifier): Modifier =
        (if (index == 0) modifier.focusRequester(first) else modifier).onFocusChanged { if (it.isFocused) focused = index }

    internal fun toStart() {
        scope.launch {
            list.scrollToItem(0)
            settleFor { runCatching { first.requestFocus() }.getOrDefault(false) }
        }
    }
}

/** A row's [RowBack], with its Back handler: on while [enabled] and an item after the first has focus. */
@Composable
fun rememberRowBack(list: LazyListState, enabled: Boolean = true): RowBack {
    val scope = rememberCoroutineScope()
    val back = remember(list) { RowBack(list, scope) }
    BackHandler(enabled = enabled && back.pastFirst) { back.toStart() }
    return back
}

/**
 * Left and Right never leave a row (the owner, 2026-11-15): off a row's end the focus stays on its last item, where
 * it went up to the top bar before. Up and Down still move between the rows. Put it on the row's list.
 */
fun Modifier.staysInRow(): Modifier = focusProperties {
    onExit = {
        if (requestedFocusDirection == FocusDirection.Left || requestedFocusDirection == FocusDirection.Right) cancelFocusChange()
    }
}.focusGroup()

/** When Home was last reset (see Navigator.homeReset): 0 outside Home, and on Home until the first reset. */
val LocalHomeReset = compositionLocalOf { 0L }

/**
 * A row's list state on Home that goes back to its first item at a reset of Home (see LocalHomeReset): when the reset
 * comes, or when the row comes into view after one.
 */
@Composable
fun rememberHomeRowState(): LazyListState {
    val state = rememberLazyListState()
    val reset = LocalHomeReset.current
    var seen by rememberSaveable { mutableLongStateOf(reset) }
    LaunchedEffect(reset) {
        if (reset <= seen) return@LaunchedEffect
        seen = reset
        state.scrollToItem(0)
    }
    return state
}
