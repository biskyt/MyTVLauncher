package com.biskyt.mytvlauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SettingRow
import com.biskyt.mytvlauncher.ui.common.Toggle
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import kotlinx.coroutines.launch

/**
 * Settings › Home rows (the owner, 2026-11-14). Continue watching on or off, each app that has items with its own
 * switch, and the items hidden one by one, to show again. Then the apps' channel rows on or off, and each channel's
 * switch. When a row is off, the launcher reads nothing for it from the TV provider.
 */
@Composable
fun HomeRowsScreen() {
    val container = LocalContext.current.container
    val watchNext = container.watchNext
    val watchOn by container.watchNextShown.collectAsState()
    val appSwitches by remember { watchNext.appSwitches(container.appLabels) }.collectAsState(initial = emptyList())
    val hiddenItems by watchNext.hiddenItems.collectAsState(initial = 0)
    val channelsOn by container.channelsShown.collectAsState()
    val channelSwitches by remember { container.channels.switches(container.appLabels) }.collectAsState(initial = emptyList())
    val watch = watchOn ?: return
    val channels = channelsOn ?: return
    fun later(block: suspend () -> Unit) {
        container.appScope.launch { block() }
    }
    SettingsPage(R.string.settings_home_rows) {
        item {
            SettingRow(
                stringResource(R.string.home_continue_watching),
                stringResource(R.string.rows_continue_watching_help),
                { later { container.settings.setWatchNextShown(!watch) } },
                rememberFirstFocus(once = true)
            ) { Toggle(watch) }
        }
        if (watch) {
            heading(R.string.rows_apps)
            if (appSwitches.isEmpty()) item { Muted(stringResource(R.string.rows_no_apps)) }
            items(appSwitches, key = { "app-${it.packageName}" }) { app ->
                SettingRow(app.label, null, { later { watchNext.setAppShown(app.packageName, !app.on) } }) { Toggle(app.on) }
            }
            if (hiddenItems > 0) {
                item {
                    SettingRow(
                        stringResource(R.string.rows_hidden_items, hiddenItems),
                        stringResource(R.string.rows_hidden_items_help),
                        { later { watchNext.showAllItems() } }
                    )
                }
            }
        }
        item {
            SettingRow(
                stringResource(R.string.rows_channels),
                stringResource(R.string.rows_channels_help),
                { later { container.settings.setChannelsShown(!channels) } },
                Modifier.padding(top = 12.dp)
            ) { Toggle(channels) }
        }
        if (channels) {
            if (channelSwitches.isEmpty()) item { Muted(stringResource(R.string.rows_no_channels)) }
            items(channelSwitches, key = { "channel-${it.id}" }) { channel ->
                SettingRow(channel.name, channel.app, { later { container.channels.setChannelShown(channel.id, !channel.on) } }) {
                    Toggle(channel.on)
                }
            }
        }
    }
}

private fun LazyListScope.heading(text: Int) = item {
    Text(stringResource(text), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
}
