package com.biskyt.mytvlauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.recents.AppUse
import com.biskyt.mytvlauncher.core.recents.RecentApps
import com.biskyt.mytvlauncher.data.AppEntry
import com.biskyt.mytvlauncher.data.UsageStatsRecents
import com.biskyt.mytvlauncher.openOrSay
import com.biskyt.mytvlauncher.openUsageAccess
import com.biskyt.mytvlauncher.ui.Navigator
import com.biskyt.mytvlauncher.ui.Screen
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SettingRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One read of the usage stats: [uses] is null without usage access. */
private class UsageRead(val uses: List<AppUse>?)

/** What the recent apps screen shows: the apps (most recent first), or that usage access is needed. */
private sealed interface Recents {
    data class Apps(val packageNames: List<String>) : Recents

    data object NoAccess : Recents
}

/**
 * The recent apps, opened by pressing Home twice (the owner, 2026-11-14). Read from Android's usage stats in the
 * background each time it opens. Without usage access, it says how to give it. Under the apps, a tile opens All apps
 * (the owner, 2026-11-15). Back goes to Home.
 */
@Composable
fun RecentsScreen(nav: Navigator) {
    val context = LocalContext.current
    val container = context.container
    val labels by container.appLabels.collectAsState()
    val apps by container.allApps.collectAsState()
    // The app that the remote held OK (or Menu) on (the owner, 2026-11-15): its options, as on Home but with no Move.
    var options by remember { mutableStateOf<AppEntry?>(null) }
    // Read in the background each time the screen comes to the front (usage access may have been given meanwhile); the
    // list is filtered again as the apps change.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateFlow.collectAsState()
    val inFront = state.isAtLeast(Lifecycle.State.RESUMED)
    val read by produceState<UsageRead?>(null, inFront) {
        if (!inFront) return@produceState
        value = withContext(Dispatchers.IO) { UsageRead(if (container.recents.allowed()) container.recents.uses() else null) }
    }
    val recents = remember(read, labels) {
        read?.let { r -> r.uses?.let { Recents.Apps(RecentApps.order(it, labels.keys)) } ?: Recents.NoAccess }
    }
    val first = rememberOnceFocus(ready = recents != null)
    val missing = stringResource(R.string.recents_access_missing)
    Column(Modifier.fillMaxSize().padding(horizontal = ROW_PAD), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle(stringResource(R.string.recents_title))
        when (val r = recents) {
            null -> Unit
            Recents.NoAccess -> {
                SettingRow(
                    stringResource(R.string.recents_access),
                    stringResource(R.string.recents_access_help),
                    { context.openOrSay(missing) { openUsageAccess() } },
                    Modifier.width(820.dp).focusOn(first)
                )
                AdbCommand(stringResource(R.string.recents_adb), UsageStatsRecents.adbGrant(context.packageName))
            }
            is Recents.Apps -> if (r.packageNames.isEmpty()) {
                Muted(stringResource(R.string.recents_none))
            } else {
                // Room on every side for the tile with focus to grow and glow into (the row cuts off what is outside
                // it), moved left by as much, so that the first tile stays in line with the text above.
                LazyRow(
                    Modifier.offset(x = -GLOW_ROOM),
                    contentPadding = PaddingValues(GLOW_ROOM),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(r.packageNames, key = { _, name -> name }) { i, name ->
                        AppTile(
                            name,
                            labels[name] ?: name,
                            modifier = if (i == 0) Modifier.focusOn(first) else Modifier,
                            onOptions = { options = apps?.firstOrNull { it.packageName == name } }
                        )
                    }
                }
            }
        }
        if (recents != null) {
            // The first focus when there is no recent app to give it to.
            val noApps = recents is Recents.Apps && recents.packageNames.isEmpty()
            AllAppsTile({ nav.open(Screen.ALL_APPS) }, if (noApps) Modifier.focusOn(first) else Modifier)
        }
    }
    options?.let { app -> AppOptions(app, onMove = null) { options = null } }
}
