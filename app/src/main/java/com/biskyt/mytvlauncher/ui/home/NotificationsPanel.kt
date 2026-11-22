package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.notifications.TvNotification
import com.biskyt.mytvlauncher.ui.common.BoardPanel
import com.biskyt.mytvlauncher.ui.common.BoardPanelTitle
import com.biskyt.mytvlauncher.ui.common.PillButton
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.theme.Palette

/**
 * The system notifications (the owner, 2026-11-15), newest first, from the top bar's bell. Each one is a card as wide
 * as the panel, with Open and Dismiss buttons (the owner, 2026-11-22). Open opens it, as the TV's own panel does, and
 * closes the panel. Dismiss takes it away. The panel closes when the last one goes.
 */
@Composable
fun NotificationsPanel(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val notices by container.notifications.collectAsState()
    val labels by container.appLabels.collectAsState()
    val notOpened = stringResource(R.string.notifications_not_opened)
    BoardPanel(onDismiss) {
        BoardPanelTitle(stringResource(R.string.notifications_title))
        notices.forEachIndexed { i, notice ->
            // Keyed: a new notice at the top must not move the focus onto another notice.
            key(notice.key) {
                NotificationCard(
                    notice,
                    labels[notice.packageName] ?: notice.packageName,
                    if (i == 0) rememberFirstFocus() else Modifier,
                    onOpen = {
                        onDismiss()
                        if (!container.notificationSource.open(notice.key)) Toast.makeText(context, notOpened, Toast.LENGTH_SHORT).show()
                    },
                    onRemove = { container.notificationSource.dismiss(notice.key) }
                )
            }
        }
    }
}

/** One notification: its app, title and text, and under them Open (with [openFocus]) and Dismiss. */
@Composable
private fun NotificationCard(notice: TvNotification, app: String, openFocus: Modifier, onOpen: () -> Unit, onRemove: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Palette.surfaceLow, CardShape).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(app, style = MaterialTheme.typography.labelSmall, color = Palette.textDim)
        notice.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, color = Palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        notice.text?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(stringResource(R.string.notifications_open), onOpen, openFocus)
            PillButton(stringResource(R.string.notifications_dismiss), onRemove)
        }
    }
}

private val CardShape = RoundedCornerShape(10.dp)
