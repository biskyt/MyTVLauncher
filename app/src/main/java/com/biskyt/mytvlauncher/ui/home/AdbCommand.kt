package com.biskyt.mytvlauncher.ui.home

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.theme.LocalAccent

/** For a TV that does not ask for a permission: [help], then the adb [command] that gives it, in the accent colour. */
@Composable
fun AdbCommand(help: String, command: String) {
    Muted(help, maxLines = 2)
    Text(command, style = MaterialTheme.typography.bodyMedium, color = LocalAccent.current.soft)
}
