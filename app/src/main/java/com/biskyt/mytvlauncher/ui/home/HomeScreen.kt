package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.apps.AppOrder
import com.biskyt.mytvlauncher.data.AppEntry
import com.biskyt.mytvlauncher.data.TvListings
import com.biskyt.mytvlauncher.openAndroidSettings
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
import kotlinx.coroutines.launch

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
    val channelsOn by container.channelsShown.collectAsState()
    val channels by container.channelRows.collectAsState()
    val allowed by container.watchNext.allowed.collectAsState()
    val channelsAllowed by container.channels.allowed.collectAsState()
    val context = LocalContext.current
    val refused = stringResource(R.string.home_allow_refused)
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.watchNext.refresh()
        container.channels.refresh()
        // Refused (or a TV with no question for it): the way that is left is adb (Settings › Watch Next check).
        if (!it) Toast.makeText(context, refused, Toast.LENGTH_LONG).show()
    }
    var options by remember { mutableStateOf<ItemChoice?>(null) }
    // Drawn once every saved list is read (from Room, and the settings that the accent colour waits for already).
    val appList = apps
    val watch = items
    val on = rowOn
    val ready = appList != null && watch != null && on != null && channelRowsRead(channelsOn, channels)
    // The permission is asked for while either row that needs it is on.
    val askFirst = (on == true || channelsOn == true) && !allowed
    val watchRow = on == true && allowed && !watch.isNullOrEmpty()
    // Focus starts on the top row's first card, once a visit: never again while the remote moves along the rows.
    val first = rememberOnceFocus(ready)
    val toApps = !askFirst && !watchRow
    Column(Modifier.fillMaxSize()) {
        TopBar(nav)
        if (!ready) return@Column
        LazyColumn(contentPadding = PaddingValues(top = 22.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            if (askFirst) item(key = "allow") { AllowRow(first) { ask.launch(TvListings.PERMISSION) } }
            if (watchRow) {
                item(key = "watch") {
                    ContinueWatchingRow(watch.orEmpty(), first) { options = ItemChoice(it) }
                }
            }
            item(key = "apps") { AppsRow(appList.orEmpty(), if (toApps) first else null) { nav.open(Screen.ALL_APPS) } }
            // Below the apps (layout B), while the setting is on; they arrive from the saved rows as Room reads them.
            if (channelsOn == true && channelsAllowed) {
                channels.orEmpty().forEach { channel ->
                    item(key = "channel-${channel.id}") { ChannelRow(channel) { options = ItemChoice(it, channel.id) } }
                }
            }
        }
    }
    options?.let { ItemOptions(it) { options = null } }
}

/**
 * The apps row: every app on Home, as square icons with their names, then the All apps tile. Holding OK (or Menu) on
 * an app opens its options; Move lifts it, and Left and Right move it until OK or Back, when the order is saved.
 */
@Composable
private fun AppsRow(apps: List<AppEntry>, firstFocus: FocusRequester?, onAllApps: () -> Unit) {
    val container = LocalContext.current.container
    var options by remember { mutableStateOf<AppEntry?>(null) }
    var moving by remember { mutableStateOf<String?>(null) }
    var order by remember { mutableStateOf(emptyList<String>()) }
    // A moved order being saved: shown until the saved list comes back, so that the row does not flick back meanwhile.
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(apps) { saving = false }
    val byName = remember(apps) { apps.associateBy { it.packageName } }
    val listState = rememberLazyListState()
    fun endMove() {
        val moved = order
        moving = null
        saving = true
        container.appScope.launch { container.apps.saveOrder(moved) }
    }
    BackHandler(enabled = moving != null) { endMove() }
    Column {
        SectionTitle(stringResource(R.string.home_apps), Modifier.padding(horizontal = ROW_PAD))
        moving?.let { name ->
            Muted(stringResource(R.string.app_move_hint, byName[name]?.label ?: name), Modifier.padding(start = ROW_PAD, bottom = 10.dp))
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = ROW_PAD),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val shown = if (moving != null || saving) order.mapNotNull { byName[it] } else apps
            itemsIndexed(shown, key = { _, app -> app.packageName }) { i, app ->
                val isMoving = moving == app.packageName
                AppTile(
                    app.packageName,
                    app.label,
                    modifier = tileModifier(i == 0, firstFocus, isMoving) { step ->
                        // The row stays where it is (a keyed row would follow the first app shown as it moves).
                        listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                        order = AppOrder.move(order, app.packageName, step)
                    },
                    moving = isMoving,
                    onClick = if (isMoving) ::endMove else null,
                    onOptions = if (moving == null) ({ options = app }) else null
                )
            }
            val allAppsFocus = if (apps.isEmpty()) firstFocus else null
            if (moving == null) item(key = "all-apps") { AllAppsTile(onAllApps, Modifier.focusOn(allAppsFocus)) }
        }
    }
    options?.let { app ->
        AppOptions(app, onMove = {
            order = apps.map { it.packageName }
            moving = app.packageName
        }) { options = null }
    }
}

/** An app tile's modifier on Home: the first focus on the first tile, and move mode's keys on the app being moved. */
private fun tileModifier(first: Boolean, firstFocus: FocusRequester?, moving: Boolean, onStep: (Int) -> Unit): Modifier {
    val focus = if (first) Modifier.focusOn(firstFocus) else Modifier
    return if (moving) focus.moveKeys(onStep) else focus
}

/**
 * The top bar: "Home", then All apps, the launcher's settings and Android settings. The launcher's is named "Launcher
 * settings": Android's own Settings app is on Home too, and a second "Settings" there would be unclear.
 */
@Composable
private fun TopBar(nav: Navigator) {
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
        NetworkStatus()
        Clock()
        BarItem({ nav.open(Screen.ALL_APPS) }, LauncherIcons.Grid, stringResource(R.string.home_all_apps))
        BarItem({ nav.open(Screen.SETTINGS) }, LauncherIcons.Tune, stringResource(R.string.home_settings))
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
private fun BarItem(onClick: () -> Unit, icon: ImageVector, label: String) {
    TvCard(
        onClick = onClick,
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

private val BAR_PAD = 10.dp

/** Whether the saved channel rows are read: the setting, and the rows while it is on. */
private fun channelRowsRead(on: Boolean?, rows: List<*>?) = on == false || (on == true && rows != null)
