package com.biskyt.mytvlauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.speaker.WakeDelay
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.SettingRow
import com.biskyt.mytvlauncher.ui.common.Toggle
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import kotlinx.coroutines.launch

/**
 * Settings › Speaker wake (the owner, 2026-11-14): the speaker wake on or off, the delay after the TV wakes (OK steps
 * through the choices), and the sound now, to try it.
 */
@Composable
fun SpeakerWakeScreen() {
    val container = LocalContext.current.container
    val on by remember { container.settings.speakerWake }.collectAsState(initial = null)
    val delay by remember { container.settings.speakerWakeDelay }.collectAsState(initial = null)
    val wake = on ?: return
    val seconds = delay ?: return
    fun later(block: suspend () -> Unit) {
        container.appScope.launch { block() }
    }
    SettingsPage(R.string.speaker_title) {
        item { Muted(stringResource(R.string.speaker_intro), maxLines = 3) }
        item {
            SettingRow(
                stringResource(R.string.speaker_on),
                stringResource(R.string.speaker_on_help),
                { later { container.settings.setSpeakerWake(!wake) } },
                rememberFirstFocus(once = true)
            ) { Toggle(wake) }
        }
        item {
            SettingRow(stringResource(R.string.speaker_delay, seconds), stringResource(R.string.speaker_delay_help), {
                later { container.settings.setSpeakerWakeDelay(WakeDelay.next(seconds)) }
            })
        }
        item {
            SettingRow(stringResource(R.string.speaker_try), stringResource(R.string.speaker_try_help), {
                container.speakerWake.playNow()
            })
        }
    }
}
