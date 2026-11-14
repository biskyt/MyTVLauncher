package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.openAndroidSettings
import com.biskyt.mytvlauncher.openApp
import com.biskyt.mytvlauncher.openOrSay
import com.biskyt.mytvlauncher.ui.Navigator
import com.biskyt.mytvlauncher.ui.Screen
import com.biskyt.mytvlauncher.ui.common.LauncherIcons
import com.biskyt.mytvlauncher.ui.common.LocalScreenFocus
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SectionTitle
import com.biskyt.mytvlauncher.ui.common.TvCard
import com.biskyt.mytvlauncher.ui.common.cardColor
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import com.biskyt.mytvlauncher.ui.theme.Palette

/**
 * Home: layout B of the approved mockup (the owner, 2026-11-14). The top bar, then the rows. In 0.1.0 the only row is
 * the apps, as square icons with their names. Continue watching comes above it in 0.2.0.
 */
@Composable
fun HomeScreen(nav: Navigator) {
    val container = LocalContext.current.container
    // From the saved list: Home's first frame does not wait for a look at the installed apps (see AppRepository).
    val apps by container.shownApps.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar(nav, takeFocus = apps?.isEmpty() == true)
        Column(Modifier.padding(start = 56.dp, end = 56.dp, top = 22.dp)) {
            SectionTitle(stringResource(R.string.home_apps))
            val list = apps ?: return@Column
            if (list.isEmpty()) {
                Muted(stringResource(R.string.home_no_apps))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(list, key = { _, app -> app.packageName }) { i, app ->
                        AppTile(app, if (i == 0) rememberFirstFocus() else Modifier)
                    }
                }
            }
        }
    }
}

/** An app: its icon on a square card, and its name under it. OK opens it. */
@Composable
private fun AppTile(app: InstalledApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icons = context.container.icons
    // Read in the background, card by card; in memory after the first time.
    val icon by produceState(icons.cached(app.packageName), app.packageName) { value = icons.load(app.packageName) }
    val notOpened = stringResource(R.string.home_app_not_opened, app.label)
    Column(Modifier.width(TILE + 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TvCard(
            onClick = { context.openOrSay(notOpened, Toast.LENGTH_SHORT) { openApp(app.packageName) } },
            modifier = modifier.size(TILE).semantics { contentDescription = app.label },
            shape = RoundedCornerShape(18.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val bitmap = icon
                if (bitmap != null) {
                    Image(bitmap, contentDescription = null, modifier = Modifier.size(ICON))
                } else {
                    Text(app.label.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Palette.textDim)
                }
            }
        }
        Text(
            app.label,
            style = MaterialTheme.typography.labelMedium,
            color = Palette.textDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

/**
 * The top bar: "Home", then the launcher's Settings and Android settings. With [takeFocus] (no apps to give it to),
 * Settings takes focus.
 */
@Composable
private fun TopBar(nav: Navigator, takeFocus: Boolean) {
    val context = LocalContext.current
    val screen = LocalScreenFocus.current
    val missing = stringResource(R.string.home_android_settings_missing)
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 53.dp - BAR_PAD)
            // So that a screen given focus with none (see KeepsFocus) moves it on into its content.
            .onFocusChanged { screen?.inTopBar = it.hasFocus },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeLabel(stringResource(R.string.home_title))
        Box(Modifier.weight(1f))
        BarItem({ nav.open(Screen.SETTINGS) }, LauncherIcons.Tune, stringResource(R.string.home_settings), takeFocus)
        BarItem({
            context.openOrSay(missing) { openAndroidSettings() }
        }, LauncherIcons.Settings, stringResource(R.string.home_android_settings))
    }
}

/** "Home" in the accent colour with a line under it, as MyPlexPlayer's top bar shows the tab in use. */
@Composable
private fun HomeLabel(name: String) {
    val colour = LocalAccent.current.color
    Text(
        name,
        style = MaterialTheme.typography.labelLarge,
        color = colour,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = BAR_PAD).drawBehind {
            drawRect(colour, Offset(0f, size.height - 3.dp.toPx()), Size(size.width, 3.dp.toPx()))
        }.padding(bottom = 6.dp)
    )
}

/** A top bar button: drawn on nothing; with focus, a pill filled with the accent colour (as MyPlexPlayer's). */
@Composable
private fun BarItem(onClick: () -> Unit, icon: ImageVector, label: String, takeFocus: Boolean = false) {
    TvCard(
        onClick = onClick,
        modifier = if (takeFocus) rememberFirstFocus() else Modifier,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        fill = true
    ) {
        Icon(icon, contentDescription = label, tint = cardColor(Palette.textDim), modifier = Modifier.padding(BAR_PAD).size(24.dp))
    }
}

private val TILE = 84.dp
private val ICON = 56.dp
private val BAR_PAD = 10.dp
