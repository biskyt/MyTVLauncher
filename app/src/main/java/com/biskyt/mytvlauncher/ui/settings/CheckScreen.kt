package com.biskyt.mytvlauncher.ui.settings

import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.keys.HomePress
import com.biskyt.mytvlauncher.data.ListingsCheck
import com.biskyt.mytvlauncher.data.TableCount
import com.biskyt.mytvlauncher.data.TvListings
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SettingRow
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Watch Next check: whether the launcher may read other apps' Watch Next items and channels, and how many it can
 * read. Then the Home key test: each Home press that reaches the launcher, so the owner can see what a held press and
 * a double press send on each TV. Home does not leave this screen (see Navigator.home).
 */
@Composable
fun CheckScreen() {
    val container = LocalContext.current.container
    var reads by remember { mutableIntStateOf(0) }
    var check by remember { mutableStateOf<ListingsCheck?>(null) }
    LaunchedEffect(reads) { check = container.listings.check() }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { reads++ }
    val presses by container.homePresses.collectAsState()
    val format = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    SettingsPage(R.string.settings_check) {
        item {
            val allowed = check?.allowed
            SettingRow(
                stringResource(R.string.check_permission),
                allowed?.let { stringResource(if (it) R.string.check_allowed else R.string.check_not_allowed) },
                { if (allowed == false) ask.launch(TvListings.PERMISSION) else reads++ },
                rememberFirstFocus()
            )
        }
        item {
            Column {
                Muted(stringResource(R.string.check_adb), maxLines = 2)
                Text(
                    TvListings.adbGrant(LocalContext.current.packageName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAccent.current.soft
                )
            }
        }
        check?.let { c ->
            item { CountLine(R.string.check_watch_next, c.watchNext) }
            item { CountLine(R.string.check_channels, c.appChannels) }
            item { CountLine(R.string.check_tv_channels, c.tvChannels) }
        }
        item { SettingRow(stringResource(R.string.check_read_again), null, { reads++ }) }
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.check_home_title), style = MaterialTheme.typography.titleLarge)
                Muted(stringResource(R.string.check_home_help), maxLines = 2)
            }
        }
        if (presses.isEmpty()) {
            item { Muted(stringResource(R.string.check_home_none)) }
        }
        items(presses, key = { it.atMs }) { PressLine(it, format) }
    }
}

@Composable
private fun CountLine(name: Int, table: TableCount) {
    val label = stringResource(name)
    val text = when {
        table.error != null -> stringResource(R.string.check_refused, label, table.error)
        table.count == null -> stringResource(R.string.check_no_table, label)
        else -> stringResource(R.string.check_count, label, table.count)
    }
    Text(text, style = MaterialTheme.typography.titleSmall)
}

/** One press: the time of day it came, the gap since the one before, and what was in front. */
@Composable
private fun PressLine(press: HomePress, format: SimpleDateFormat) {
    val time = remember(press.atMs) {
        val wall = System.currentTimeMillis() - (SystemClock.uptimeMillis() - press.atMs)
        format.format(Date(wall))
    }
    val parts = listOfNotNull(
        time,
        press.gapMs?.let { stringResource(R.string.check_home_gap, it.toInt()) },
        stringResource(if (press.inFront) R.string.check_home_in_front else R.string.check_home_other_app)
    )
    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
}
