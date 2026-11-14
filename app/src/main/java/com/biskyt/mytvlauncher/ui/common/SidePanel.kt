package com.biskyt.mytvlauncher.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.biskyt.mytvlauncher.hideSystemBars
import com.biskyt.mytvlauncher.ui.theme.Palette

/* SidePanel, copied from MyPlexPlayer's ui/common/Schedule.kt (the rest of that file is MyPlexPlayer's own). */

/**
 * A panel sliding in from the right over a dimmed screen. Shown as a dialog so focus stays inside it and Back closes it.
 */
@Composable
fun SidePanel(
    onDismiss: () -> Unit,
    width: Dp = 430.dp,
    dim: Boolean = true,
    padding: PaddingValues = PaddingValues(28.dp),
    content: @Composable () -> Unit
) {
    // A dialog is its own window, which resets the density to the system's and the context to the Activity's; keep
    // the app's scaling (see MyPlexTheme) and language (see InAppLanguage) so panels match everything else.
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // Not the screen's: an item in a panel must not become the one the screen gives focus back to.
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalReturnFocus provides null
        ) {
            // A dialog is its own window: on a phone its bars would come back over the app otherwise.
            if (LocalTouch.current) {
                val dialogView = LocalView.current
                SideEffect { (dialogView.parent as? DialogWindowProvider)?.window?.hideSystemBars() }
            }
            Box(
                Modifier.fillMaxSize().ignoreHeldButtons()
                    // On a touch screen, tapping beside the panel closes it.
                    .onPlainTap(onDismiss)
                    .background(if (dim) Palette.background.copy(alpha = 0.6f) else Color.Transparent),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier.fillMaxHeight().width(width)
                        .onPlainTap {} // taps on the panel itself don't close it
                        .background(Palette.surface.copy(alpha = 0.97f)).padding(padding)
                ) { content() }
            }
        }
    }
}
