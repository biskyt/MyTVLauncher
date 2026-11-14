package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowRgb565
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.openApp
import com.biskyt.mytvlauncher.openOrSay
import com.biskyt.mytvlauncher.openWatchNext
import com.biskyt.mytvlauncher.ui.common.BoardOption
import com.biskyt.mytvlauncher.ui.common.BoardPanel
import com.biskyt.mytvlauncher.ui.common.BoardPanelSubtitle
import com.biskyt.mytvlauncher.ui.common.BoardPanelTitle
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.ProgressLine
import com.biskyt.mytvlauncher.ui.common.SectionTitle
import com.biskyt.mytvlauncher.ui.common.TvCard
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import com.biskyt.mytvlauncher.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * Continue watching: other apps' Watch Next items, newest first. A poster card for an item with a 2:3 picture, else
 * a wide card (the owner, 2026-11-14), each with its progress and its app's icon. OK opens the item; holding OK (or
 * Menu) opens its options. The first card has focus first.
 */
@Composable
fun ContinueWatchingRow(items: List<WatchNextItem>, firstFocus: FocusRequester?, onOptions: (WatchNextItem) -> Unit) {
    Column {
        SectionTitle(stringResource(R.string.home_continue_watching), Modifier.padding(horizontal = ROW_PAD))
        LazyRow(contentPadding = PaddingValues(horizontal = ROW_PAD), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(items, key = { _, item -> item.key }) { i, item ->
                WatchNextCard(item, { onOptions(item) }, if (i == 0) Modifier.focusOn(firstFocus) else Modifier)
            }
        }
    }
}

/** Without the permission: one card that asks for it, in the row's place. */
@Composable
fun AllowRow(firstFocus: FocusRequester?, onAsk: () -> Unit) {
    Column(Modifier.padding(horizontal = ROW_PAD)) {
        SectionTitle(stringResource(R.string.home_continue_watching))
        TvCard(onClick = onAsk, modifier = Modifier.focusOn(firstFocus).width(WIDE), fill = true) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(stringResource(R.string.home_allow_watch_next), style = MaterialTheme.typography.titleSmall)
                Muted(stringResource(R.string.home_allow_watch_next_help), maxLines = 2)
            }
        }
    }
}

@Composable
private fun WatchNextCard(item: WatchNextItem, onOptions: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val width = if (item.shape == CardShape.POSTER) POSTER else WIDE
    val notOpened = stringResource(R.string.home_app_not_opened, item.title)
    Column(Modifier.width(width)) {
        TvCard(
            onClick = { context.openOrSay(notOpened, Toast.LENGTH_SHORT) { openWatchNext(item) } },
            onLongClick = onOptions,
            modifier = modifier.size(width, HEIGHT).semantics { contentDescription = item.title },
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                Picture(item.pictureUri, width) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.textDim,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                AppBadge(item.packageName, Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 12.dp))
                item.progress?.let { ProgressLine(it, Modifier.align(Alignment.BottomCenter), height = 4.dp) }
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        item.subtitle?.let { Muted(it) }
    }
}

/**
 * The item's picture, decoded at the card's size (not the app's full size) and cropped to fill it, with [placeholder]
 * until it loads, or if it has none or fails (as MyPlexPlayer's ImageWithInitials).
 */
@Composable
private fun Picture(uri: String?, width: Dp, placeholder: @Composable () -> Unit) {
    var loaded by remember(uri) { mutableStateOf(false) }
    if (!loaded) placeholder()
    if (uri == null) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val request = remember(uri, width) {
        with(density) {
            ImageRequest.Builder(context).data(uri).size(width.roundToPx(), HEIGHT.roundToPx()).allowRgb565(true).build()
        }
    }
    AsyncImage(
        request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { loaded = true },
        modifier = Modifier.fillMaxSize()
    )
}

/** The app's icon, small, on the card: which app the item opens in. */
@Composable
private fun AppBadge(packageName: String, modifier: Modifier) {
    val icon = rememberAppIcon(packageName)
    icon?.let { Image(it, contentDescription = null, modifier = modifier.size(24.dp).clip(RoundedCornerShape(6.dp))) }
}

/** An item's options: open its app, hide the item, or hide all of its app's items (Settings › Home rows undoes it). */
@Composable
fun ItemOptions(item: WatchNextItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val labels by container.appLabels.collectAsState()
    val app = labels[item.packageName] ?: item.packageName
    val notOpened = stringResource(R.string.home_app_not_opened, app)
    BoardPanel(onDismiss) {
        BoardPanelTitle(item.title)
        BoardPanelSubtitle(item.subtitle ?: app)
        BoardOption(stringResource(R.string.item_open_app, app), modifier = rememberFirstFocus()) {
            onDismiss()
            context.openOrSay(notOpened, Toast.LENGTH_SHORT) { openApp(item.packageName) }
        }
        BoardOption(stringResource(R.string.item_hide)) {
            onDismiss()
            container.appScope.launch { container.watchNext.hideItem(item.key) }
        }
        BoardOption(stringResource(R.string.item_hide_app, app), stringResource(R.string.item_hide_app_help)) {
            onDismiss()
            container.appScope.launch { container.watchNext.setAppShown(item.packageName, false) }
        }
    }
}

private val POSTER = 124.dp
private val WIDE = 331.dp
private val HEIGHT = 186.dp
