package com.biskyt.mytvlauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.data.AppEntry

/**
 * All apps: every app, hidden ones too, faded and marked "Hidden from Home" (the owner, 2026-11-14: no counts and no
 * button to Android's app list). OK opens an app; holding OK (or Menu) shows it on Home again or hides it, or opens
 * App info.
 */
@Composable
fun AllAppsScreen() {
    val apps by LocalContext.current.container.allApps.collectAsState()
    var options by remember { mutableStateOf<AppEntry?>(null) }
    val list = apps
    val first = rememberOnceFocus(ready = list != null)
    val hiddenNote = stringResource(R.string.all_apps_hidden)
    Column(Modifier.fillMaxSize().padding(horizontal = ROW_PAD)) {
        ScreenTitle(stringResource(R.string.home_all_apps), Modifier.padding(bottom = 20.dp))
        LazyVerticalGrid(
            GridCells.Adaptive(COLUMN),
            contentPadding = PaddingValues(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(list.orEmpty(), key = { _, app -> app.packageName }) { i, app ->
                AppTile(
                    app.packageName,
                    app.label,
                    modifier = if (i == 0) Modifier.focusOn(first) else Modifier,
                    dimmed = app.hidden,
                    note = if (app.hidden) hiddenNote else null,
                    onOptions = { options = app }
                )
            }
        }
    }
    options?.let { AppOptions(it, onMove = null) { options = null } }
}

private val COLUMN = 110.dp
