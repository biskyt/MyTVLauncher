package com.biskyt.mytvlauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.data.AppEntry
import com.biskyt.mytvlauncher.ui.common.Muted

/**
 * All apps: every app, hidden ones too, faded and marked "Hidden from Home" (the owner, 2026-11-14: no counts and no
 * button to Android's app list). OK opens an app; holding OK (or Menu) shows it on Home again or hides it, moves it, or
 * opens App info. In move mode the arrows move the app (the owner, 2026-11-14), and OK or Back saves the order, which
 * is Home's order too.
 */
@Composable
fun AllAppsScreen() {
    val container = LocalContext.current.container
    val apps by container.allApps.collectAsState()
    var options by remember { mutableStateOf<AppEntry?>(null) }
    val list = apps
    val byName = remember(list) { list.orEmpty().associateBy { it.packageName } }
    val first = rememberOnceFocus(ready = list != null)
    val hiddenNote = stringResource(R.string.all_apps_hidden)
    val grid = rememberLazyGridState()
    val margin = with(LocalDensity.current) { MOVE_MARGIN.toPx() }
    val move = rememberListMove(list, container.apps::saveAllOrder) { name, index -> grid.keepInView(name, index, margin) }
    val moving = move.moving
    Column(Modifier.fillMaxSize().padding(horizontal = ROW_PAD)) {
        ScreenTitle(stringResource(R.string.home_all_apps), Modifier.padding(bottom = if (moving == null) 20.dp else 8.dp))
        moving?.let { name ->
            Muted(stringResource(R.string.app_move_all_hint, byName[name]?.label ?: name), Modifier.padding(bottom = 12.dp))
        }
        LazyVerticalGrid(
            GridCells.Adaptive(COLUMN),
            state = grid,
            contentPadding = PaddingValues(top = GLOW_ROOM, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(move.shown(list.orEmpty(), byName), key = { _, app -> app.packageName }) { i, app ->
                val isMoving = moving == app.packageName
                val focus = if (i == 0) Modifier.focusOn(first) else Modifier
                AppTile(
                    app.packageName,
                    app.label,
                    modifier = if (isMoving) {
                        // Up and Down move a grid row: as many apps as the grid has columns.
                        focus.moveKeys(rowStep = { grid.layoutInfo.maxSpan }) { step ->
                            move.step(app.packageName, step) {
                                grid.requestScrollToItem(grid.firstVisibleItemIndex, grid.firstVisibleItemScrollOffset)
                            }
                        }
                    } else {
                        focus
                    },
                    moving = isMoving,
                    dimmed = app.hidden,
                    note = if (app.hidden) hiddenNote else null,
                    onClick = if (isMoving) move::end else null,
                    onOptions = if (moving == null) ({ options = app }) else null
                )
            }
        }
    }
    options?.let { app ->
        AppOptions(app, onMove = {
            move.start(list.orEmpty().map { it.packageName }, app.packageName)
        }, moveHelp = R.string.app_move_all_help) {
            options = null
        }
    }
}

private val COLUMN = 110.dp

/** How far from the grid's top and bottom the app being moved stays. */
private val MOVE_MARGIN = 24.dp
