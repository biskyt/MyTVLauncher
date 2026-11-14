package com.biskyt.mytvlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.TvCard
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import kotlinx.coroutines.launch

/** The accent colour: MyPlexPlayer's grid of presets. The one in use is marked, and has focus first. */
@Composable
fun AccentScreen() {
    val container = LocalContext.current.container
    val current by container.accent.collectAsState()
    SettingsPage(R.string.settings_accent) {
        item { Muted(stringResource(R.string.settings_accent_help), maxLines = 2) }
        item { AccentGrid(current) { argb -> container.appScope.launch { container.settings.setAccent(argb) } } }
    }
}

/** MyPlexPlayer's grid of presets; [current] is marked, and has focus first. */
@Composable
private fun AccentGrid(current: Int?, onChoose: (Int) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(140.dp)
    ) {
        items(AccentPalette.presets) { (id, argb) ->
            val selected = current == argb
            TvCard(
                onClick = { onChoose(argb) },
                selected = selected,
                modifier = if (selected) rememberFirstFocus(once = true) else Modifier
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(argb)))
                    Text(
                        stringResource(accentName(id)),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }
    }
}
