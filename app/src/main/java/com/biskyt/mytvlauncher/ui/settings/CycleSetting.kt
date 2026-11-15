package com.biskyt.mytvlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.ui.common.SettingRow
import com.biskyt.mytvlauncher.ui.common.cardColor
import com.biskyt.mytvlauncher.ui.theme.Palette

/**
 * A setting shown as "◀ [value] ▶", as MyTVPlayer's (its language setting, the owner, 2026-11-15): Left and Right step it
 * back and forward ([onStep] gets -1 or 1), once a press; OK, or a tap, moves it on.
 */
@Composable
fun CycleSettingRow(label: String, help: String?, value: String, onStep: (Int) -> Unit, modifier: Modifier = Modifier) {
    val keys = Modifier.onPreviewKeyEvent { e ->
        val step = when (e.key) {
            Key.DirectionLeft -> -1
            Key.DirectionRight -> 1
            else -> return@onPreviewKeyEvent false
        }
        if (e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0) onStep(step)
        true
    }
    SettingRow(label, help, { onStep(1) }, modifier.then(keys)) { ValueChip(value) }
}

/** "◀ value ▶" chip for values changed with Left and Right (or OK to step on). */
@Composable
fun ValueChip(value: String) {
    Text(
        "◀ $value ▶",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(cardColor(Palette.surfaceHigh, CHIP_ON_ACCENT))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** On a card's accent fill: a value chip faint. */
private const val CHIP_ON_ACCENT = 0.14f
