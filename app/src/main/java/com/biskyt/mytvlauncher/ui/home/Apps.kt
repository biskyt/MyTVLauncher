package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
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
import com.biskyt.mytvlauncher.data.AppEntry
import com.biskyt.mytvlauncher.openApp
import com.biskyt.mytvlauncher.openAppInfo
import com.biskyt.mytvlauncher.openInPlayStore
import com.biskyt.mytvlauncher.openOrSay
import com.biskyt.mytvlauncher.ui.common.BoardOption
import com.biskyt.mytvlauncher.ui.common.BoardPanel
import com.biskyt.mytvlauncher.ui.common.BoardPanelTitle
import com.biskyt.mytvlauncher.ui.common.LauncherIcons
import com.biskyt.mytvlauncher.ui.common.TvCard
import com.biskyt.mytvlauncher.ui.common.cardColor
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import com.biskyt.mytvlauncher.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * An app: its icon on a square card, and its name under it. OK opens it, or runs [onClick]; holding OK (or Menu) runs
 * [onOptions]. [moving] lifts it with an accent ring (move mode). [onHome] marks it with a heart (All apps).
 */
@Composable
fun AppTile(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    moving: Boolean = false,
    onHome: Boolean = false,
    onClick: (() -> Unit)? = null,
    onOptions: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(packageName)
    val notOpened = stringResource(R.string.home_app_not_opened, label)
    val ring = if (moving) Modifier.border(BorderStroke(3.dp, LocalAccent.current.color), TileShape) else Modifier
    Column(
        Modifier.raiseOnFocus().width(TILE + 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The heart is outside the card, over the circle's top right, so that the round card does not cut it off.
        Box {
            TvCard(
                onClick = onClick ?: { context.openOrSay(notOpened, Toast.LENGTH_SHORT) { openApp(packageName) } },
                onLongClick = onOptions,
                modifier = modifier.focusGlow(TileShape, LocalAccent.current.color).then(ring).size(TILE).semantics {
                    contentDescription = label
                },
                shape = TileShape,
                // The glow shows the focus (see focusGlow), in place of the ring.
                ring = false
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        // The icon fills the round card (the owner, 2026-11-15).
                        Image(icon, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(label.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Palette.textDim)
                    }
                }
            }
            if (onHome) {
                Icon(
                    LauncherIcons.Heart,
                    contentDescription = stringResource(R.string.all_apps_on_home),
                    tint = LocalAccent.current.color,
                    // On a dark disc, so that it shows on any icon.
                    modifier = Modifier.align(Alignment.TopEnd).size(HEART).background(Palette.background, CircleShape).padding(4.dp)
                )
            }
        }
        TileLabel(label)
    }
}

/** The tile at the end of the apps row that opens All apps (the owner, 2026-11-14). */
@Composable
fun AllAppsTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.home_all_apps)
    Column(Modifier.raiseOnFocus().width(TILE + 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TvCard(
            onClick = onClick,
            modifier = modifier.focusGlow(TileShape, LocalAccent.current.color).size(TILE).semantics {
                contentDescription = label
            },
            shape = TileShape,
            // The glow shows the focus (see focusGlow), in place of the ring.
            ring = false
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(LauncherIcons.Grid, contentDescription = null, tint = cardColor(Palette.textDim), modifier = Modifier.size(36.dp))
            }
        }
        TileLabel(label)
    }
}

@Composable
private fun TileLabel(text: String) = Text(
    text,
    style = MaterialTheme.typography.labelMedium,
    color = Palette.textDim,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
)

/**
 * Move mode's keys on the app being moved: Left and Right move it one place ([onStep] gets -1 or 1), and Up and Down
 * move it [rowStep] places (a grid row in All apps). On a row (a step of 0), Up and Down are kept.
 */
fun Modifier.moveKeys(rowStep: () -> Int = { 0 }, onStep: (Int) -> Unit): Modifier = onPreviewKeyEvent { e ->
    val step = when (e.key) {
        Key.DirectionLeft -> -1
        Key.DirectionRight -> 1
        Key.DirectionUp -> -rowStep()
        Key.DirectionDown -> rowStep()
        else -> return@onPreviewKeyEvent false
    }
    if (e.type == KeyEventType.KeyDown && step != 0) onStep(step)
    true
}

/**
 * An app's options: Move (when [onMove] is given), Remove from or Add to favourite apps (the apps on Home; the owner,
 * 2026-11-15), App info (Android's page for it), and Open in Play Store (the owner, 2026-11-15).
 */
@Composable
fun AppOptions(app: AppEntry, onMove: (() -> Unit)?, moveHelp: Int = R.string.app_move_help, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val noInfo = stringResource(R.string.app_info_missing)
    val noStore = stringResource(R.string.app_play_store_missing)
    BoardPanel(onDismiss) {
        BoardPanelTitle(app.label)
        if (onMove != null) {
            BoardOption(stringResource(R.string.app_move), stringResource(moveHelp), rememberFirstFocus()) {
                onDismiss()
                onMove()
            }
        }
        BoardOption(
            stringResource(if (app.hidden) R.string.app_show else R.string.app_hide),
            if (app.hidden) null else stringResource(R.string.app_hide_help),
            if (onMove == null) rememberFirstFocus() else Modifier
        ) {
            onDismiss()
            container.appScope.launch { container.apps.setHidden(app.packageName, !app.hidden) }
        }
        BoardOption(stringResource(R.string.app_info), stringResource(R.string.app_info_help)) {
            onDismiss()
            context.openOrSay(noInfo) { openAppInfo(app.packageName) }
        }
        BoardOption(stringResource(R.string.app_play_store), stringResource(R.string.app_play_store_help)) {
            onDismiss()
            context.openOrSay(noStore) { openInPlayStore(app.packageName) }
        }
    }
}

/** A full screen's title (All apps, Recent apps), as the settings pages set theirs. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.headlineLarge, modifier = modifier.padding(top = 40.dp))

/** App cards are round (the owner, 2026-11-15: "Option B"). */
private val TileShape = CircleShape
private val TILE = 84.dp
private val HEART = 24.dp
