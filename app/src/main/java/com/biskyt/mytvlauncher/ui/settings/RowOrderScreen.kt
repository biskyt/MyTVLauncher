package com.biskyt.mytvlauncher.ui.settings

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.rows.RowOrder
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SettingRow
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.home.keepInView
import com.biskyt.mytvlauncher.ui.home.moveKeys
import com.biskyt.mytvlauncher.ui.home.rememberListMove
import com.biskyt.mytvlauncher.ui.home.rememberRowPadPx

/** A row of Home in the Row order list: its key (see RowOrder), its name, and its app's name for a channel. */
private data class OrderRow(val key: String, val name: String, val app: String?)

/** The page's title and intro come before the rows. */
private const val ROWS_FROM = 2

/**
 * Settings › Home rows › Row order (the owner, 2026-11-14): the order of Home's rows, from the top. OK on a row lifts
 * it, Up and Down move it, and OK or Back saves the order. Only the rows that are on are listed; a row that is off
 * keeps its place for when it is on again.
 */
@Composable
fun RowOrderScreen() {
    val container = LocalContext.current.container
    val channels by container.channelRows.collectAsState()
    val watchOn by container.watchNextShown.collectAsState()
    val savedOrder by container.rowOrder.collectAsState()
    val labels by container.appLabels.collectAsState()
    val saved = savedOrder ?: return
    val watchName = stringResource(R.string.home_continue_watching)
    val appsName = stringResource(R.string.home_apps)
    val rows = remember(channels, watchOn, saved, labels, watchName, appsName) {
        val all =
            listOfNotNull(
                OrderRow(RowOrder.WATCH_NEXT, watchName, null).takeIf {
                    watchOn == true
                },
                OrderRow(RowOrder.APPS, appsName, null)
            ) +
                channels.orEmpty().map { OrderRow(RowOrder.channel(it.id), it.name, labels[it.packageName] ?: it.packageName) }
        val byKey = all.associateBy { it.key }
        RowOrder.order(all.map { it.key }, saved).map(byKey::getValue)
    }
    val byKey = remember(rows) { rows.associateBy { it.key } }
    val list = rememberLazyListState()
    val margin = rememberRowPadPx()
    val move = rememberListMove(saved, { container.settings.setRowOrder(RowOrder.save(it, saved)) }) { key, index ->
        list.keepInView(key, index + ROWS_FROM, margin)
    }
    val moving = move.moving
    // The first focus goes to the row that was first when the page opened, not to each row that becomes first.
    val firstKey = remember { rows.first().key }
    SettingsPage(R.string.rows_order, list) {
        item {
            val name = moving?.let { byKey[it]?.name }
            Muted(
                if (name !=
                    null
                ) {
                    stringResource(R.string.rows_order_moving, name)
                } else {
                    stringResource(R.string.rows_order_intro)
                },
                maxLines = 2
            )
        }
        items(move.shown(rows, byKey), key = { it.key }) { row ->
            val isMoving = moving == row.key
            val focus = if (row.key == firstKey) rememberFirstFocus(once = true) else Modifier
            SettingRow(
                row.name,
                row.app,
                {
                    when {
                        isMoving -> move.end()
                        moving == null -> move.start(rows.map { it.key }, row.key)
                    }
                },
                if (isMoving) focus.moveKeys(rowStep = { 1 }) { step -> move.step(row.key, step) } else focus
            ) {
                if (isMoving) Muted(stringResource(R.string.rows_order_lifted))
            }
        }
    }
}
