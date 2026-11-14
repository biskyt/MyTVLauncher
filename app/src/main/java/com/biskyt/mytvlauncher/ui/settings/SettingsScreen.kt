package com.biskyt.mytvlauncher.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.BuildConfig
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.openAndroidSettings
import com.biskyt.mytvlauncher.openHomeAppChoice
import com.biskyt.mytvlauncher.openOrSay
import com.biskyt.mytvlauncher.ui.Navigator
import com.biskyt.mytvlauncher.ui.Screen
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SettingRow
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.theme.LocalAccent

/** The launcher's settings: the accent colour, Home rows, the Watch Next check, Android settings, the home app, the version. */
@Composable
fun SettingsScreen(nav: Navigator) {
    val context = LocalContext.current
    val accent = LocalAccent.current.color
    val missing = stringResource(R.string.home_android_settings_missing)
    val noHomeChoice = stringResource(R.string.settings_home_app_missing)
    SettingsPage(R.string.settings_title) {
        item {
            SettingRow(
                stringResource(R.string.settings_accent),
                stringResource(R.string.settings_accent_help),
                { nav.open(Screen.ACCENT) },
                rememberFirstFocus(once = true)
            ) { Box(Modifier.size(26.dp).clip(CircleShape).background(accent)) }
        }
        item {
            SettingRow(stringResource(R.string.settings_home_rows), stringResource(R.string.settings_home_rows_help), {
                nav.open(Screen.HOME_ROWS)
            })
        }
        item {
            SettingRow(stringResource(R.string.settings_check), stringResource(R.string.settings_check_help), { nav.open(Screen.CHECK) })
        }
        item {
            SettingRow(stringResource(R.string.home_android_settings), stringResource(R.string.settings_android_help), {
                context.openOrSay(missing) { openAndroidSettings() }
            })
        }
        // A way back to another launcher, where the TV has one (the owner, 2026-11-14).
        item {
            SettingRow(stringResource(R.string.settings_home_app), stringResource(R.string.settings_home_app_help), {
                context.openOrSay(noHomeChoice) { openHomeAppChoice() }
            })
        }
        item {
            Column(Modifier.padding(top = 12.dp)) {
                Text("${stringResource(R.string.app_name)} · ${stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)}")
                Muted(stringResource(R.string.settings_no_tracking))
            }
        }
    }
}

/** A settings page: its title, then its rows in a column that scrolls, as MyPlexPlayer's settings pages lay them out. */
@Composable
fun SettingsPage(@StringRes title: Int, content: LazyListScope.() -> Unit) {
    LazyColumn(
        // Rows as wide as MyPlexPlayer's settings rows, not the whole screen.
        Modifier.fillMaxHeight().padding(horizontal = 96.dp).width(820.dp),
        contentPadding = PaddingValues(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(stringResource(title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 8.dp)) }
        content()
    }
}

/** The name of an accent colour offered in Settings (an AccentPalette.Preset id), as MyPlexPlayer names them. */
@StringRes
fun accentName(id: String): Int = accentNames.getValue(id)

private val accentNames = mapOf(
    "mint" to R.string.accent_mint,
    "yellow" to R.string.accent_yellow,
    "violet" to R.string.accent_violet,
    "sky" to R.string.accent_sky,
    "coral" to R.string.accent_coral,
    "lime" to R.string.accent_lime,
    "pink" to R.string.accent_pink,
    "silver" to R.string.accent_silver
)
