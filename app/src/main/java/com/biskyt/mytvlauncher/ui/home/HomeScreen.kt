package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.TvListings
import com.biskyt.mytvlauncher.openAndroidSettings
import com.biskyt.mytvlauncher.openApp
import com.biskyt.mytvlauncher.openOrSay
import com.biskyt.mytvlauncher.ui.Navigator
import com.biskyt.mytvlauncher.ui.Screen
import com.biskyt.mytvlauncher.ui.common.LauncherIcons
import com.biskyt.mytvlauncher.ui.common.LocalReturnFocus
import com.biskyt.mytvlauncher.ui.common.LocalScreenFocus
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SectionTitle
import com.biskyt.mytvlauncher.ui.common.TvCard
import com.biskyt.mytvlauncher.ui.common.cardColor
import com.biskyt.mytvlauncher.ui.common.settleFor
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import com.biskyt.mytvlauncher.ui.theme.Palette

/**
 * Home: layout B of the approved mockup (the owner, 2026-11-14). The top bar, then the rows: Continue watching, then
 * the apps as square icons with their names. Both rows come from saved lists, so the first frame waits for no read
 * of the installed apps or the TV provider. The top row's first card has focus first.
 */
@Composable
fun HomeScreen(nav: Navigator) {
    val container = LocalContext.current.container
    val apps by container.shownApps.collectAsState()
    val items by container.watchNextItems.collectAsState()
    val rowOn by container.watchNextShown.collectAsState()
    val allowed by container.watchNext.allowed.collectAsState()
    val context = LocalContext.current
    val refused = stringResource(R.string.home_allow_refused)
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.watchNext.refresh()
        // Refused (or a TV with no question for it): the way that is left is adb (Settings › Watch Next check).
        if (!it) Toast.makeText(context, refused, Toast.LENGTH_LONG).show()
    }
    var options by remember { mutableStateOf<WatchNextItem?>(null) }
    // Drawn once every saved list is read (from Room, and the settings that the accent colour waits for already).
    val appList = apps
    val watch = items
    val on = rowOn
    val ready = appList != null && watch != null && on != null
    val askFirst = on == true && !allowed
    val watchRow = on == true && allowed && !watch.isNullOrEmpty()
    // Focus starts on the top row's first card, once a visit: never again while the remote moves along the rows.
    val first = rememberOnceFocus(ready)
    val toApps = !askFirst && !watchRow && !appList.isNullOrEmpty()
    Column(Modifier.fillMaxSize()) {
        TopBar(nav, firstFocus = if (ready && !askFirst && !watchRow && !toApps) first else null)
        if (!ready) return@Column
        LazyColumn(contentPadding = PaddingValues(top = 22.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            if (askFirst) item(key = "allow") { AllowRow(first) { ask.launch(TvListings.PERMISSION) } }
            if (watchRow) item(key = "watch") { ContinueWatchingRow(watch.orEmpty(), if (watchRow) first else null) { options = it } }
            item(key = "apps") { AppsRow(appList.orEmpty(), if (toApps) first else null) }
        }
    }
    options?.let { ItemOptions(it) { options = null } }
}

/** The apps row: every app on Home, as square icons with their names. */
@Composable
private fun AppsRow(apps: List<InstalledApp>, firstFocus: FocusRequester?) {
    Column {
        SectionTitle(stringResource(R.string.home_apps), Modifier.padding(horizontal = ROW_PAD))
        if (apps.isEmpty()) {
            Muted(stringResource(R.string.home_no_apps), Modifier.padding(horizontal = ROW_PAD))
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = ROW_PAD), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { i, app ->
                    AppTile(app, if (i == 0) Modifier.focusOn(firstFocus) else Modifier)
                }
            }
        }
    }
}

/** An app: its icon on a square card, and its name under it. OK opens it. */
@Composable
private fun AppTile(app: InstalledApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon = rememberAppIcon(app.packageName)
    val notOpened = stringResource(R.string.home_app_not_opened, app.label)
    Column(Modifier.width(TILE + 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TvCard(
            onClick = { context.openOrSay(notOpened, Toast.LENGTH_SHORT) { openApp(app.packageName) } },
            modifier = modifier.size(TILE).semantics { contentDescription = app.label },
            shape = RoundedCornerShape(18.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Image(icon, contentDescription = null, modifier = Modifier.size(ICON))
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
 * The top bar: "Home", then the launcher's settings and Android settings. The first is named "Launcher settings":
 * Android's own Settings app is on Home too, and a second "Settings" there would be unclear. With [takeFocus] (no apps to give it to),
 * Settings takes focus.
 */
@Composable
private fun TopBar(nav: Navigator, firstFocus: FocusRequester?) {
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
        BarItem({ nav.open(Screen.SETTINGS) }, LauncherIcons.Tune, stringResource(R.string.home_settings), firstFocus)
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
private fun BarItem(onClick: () -> Unit, icon: ImageVector, label: String, firstFocus: FocusRequester? = null) {
    TvCard(
        onClick = onClick,
        modifier = Modifier.focusOn(firstFocus),
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        fill = true
    ) {
        Icon(icon, contentDescription = label, tint = cardColor(Palette.textDim), modifier = Modifier.padding(BAR_PAD).size(24.dp))
    }
}

/** The rows' side padding, as MyPlexPlayer's Home rows. */
internal val ROW_PAD = 56.dp

/**
 * A requester that the screen gives focus to once a visit, when [ready]: not again when its item is composed again (a
 * row scrolled back into view) or replaced (a new first item). Coming back to the screen, the item used last has focus
 * again instead (see ReturnFocus).
 */
@Composable
fun rememberOnceFocus(ready: Boolean): FocusRequester {
    val requester = remember { FocusRequester() }
    var done by rememberSaveable { mutableStateOf(false) }
    val back = LocalReturnFocus.current
    LaunchedEffect(ready) {
        if (!ready || done) return@LaunchedEffect
        if (back?.cameBack == true) {
            done = true
            return@LaunchedEffect
        }
        // The item is laid out within a frame or two of its list.
        done = settleFor { runCatching { requester.requestFocus(FocusDirection.Enter) }.getOrDefault(false) }
    }
    return requester
}

/** Attaches [requester], when there is one. */
fun Modifier.focusOn(requester: FocusRequester?): Modifier = if (requester == null) this else focusRequester(requester)

/** An app's icon, read in the background card by card, and kept in memory after the first time; null until read. */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val icons = LocalContext.current.container.icons
    val icon by produceState(icons.cached(packageName), packageName) { value = icons.load(packageName) }
    return icon
}

private val TILE = 84.dp
private val ICON = 56.dp
private val BAR_PAD = 10.dp
