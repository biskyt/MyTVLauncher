package com.biskyt.mytvlauncher.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.biskyt.mytvlauncher.core.channels.AppChannel
import com.biskyt.mytvlauncher.core.watchnext.CardShape
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import com.biskyt.mytvlauncher.data.TvListings
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
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import com.biskyt.mytvlauncher.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * Continue watching: other apps' Watch Next items, newest first. A poster card for an item with a 2:3 picture, else
 * a wide card (the owner, 2026-11-14), each with its progress and its app's icon. OK opens the item; holding OK (or
 * Menu) opens its options. The first card has focus first.
 */
@Composable
fun ContinueWatchingRow(items: List<WatchNextItem>, firstFocus: FocusRequester?, onOptions: (WatchNextItem) -> Unit) =
    CardRow(stringResource(R.string.home_continue_watching), null, items, firstFocus, CardSize.FULL, onOptions)

/**
 * An app's channel row (the owner, 2026-11-14): its items as Continue watching shows them, at half the size (the owner,
 * 2026-11-14), under its name and its app's name.
 */
@Composable
fun ChannelRow(channel: AppChannel, firstFocus: FocusRequester?, onOptions: (WatchNextItem) -> Unit) {
    val labels by LocalContext.current.container.appLabels.collectAsState()
    CardRow(channel.name, labels[channel.packageName] ?: channel.packageName, channel.items, firstFocus, CardSize.HALF, onOptions)
}

/** A titled row of item cards ([subtitle]: the app, for a channel). */
@Composable
private fun CardRow(
    title: String,
    subtitle: String?,
    items: List<WatchNextItem>,
    firstFocus: FocusRequester?,
    size: CardSize,
    onOptions: (WatchNextItem) -> Unit
) {
    Column {
        if (subtitle == null) {
            SectionTitle(title, Modifier.padding(horizontal = ROW_PAD))
        } else {
            Row(
                Modifier.padding(horizontal = ROW_PAD),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTitle(title)
                Muted(subtitle, Modifier.padding(bottom = 14.dp))
            }
        }
        val list = rememberLazyListState()
        val back = rememberRowBack(list)
        LazyRow(
            back.row,
            state = list,
            contentPadding = PaddingValues(horizontal = ROW_PAD, vertical = GLOW_ROOM),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.key }) { i, item ->
                WatchNextCard(item, size, { onOptions(item) }, back.item(i, if (i == 0) Modifier.focusOn(firstFocus) else Modifier))
            }
        }
    }
}

/**
 * Without the permission: one card that asks for it, in the row's place, and under it the adb command for a TV that
 * does not ask.
 */
@Composable
fun AllowRow(firstFocus: FocusRequester?, onAsk: () -> Unit) {
    Column(Modifier.padding(horizontal = ROW_PAD), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle(stringResource(R.string.home_continue_watching))
        TvCard(onClick = onAsk, modifier = Modifier.focusOn(firstFocus).width(WIDE), fill = true) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(stringResource(R.string.home_allow_watch_next), style = MaterialTheme.typography.titleSmall)
                Muted(stringResource(R.string.home_allow_watch_next_help), maxLines = 2)
            }
        }
        AdbCommand(stringResource(R.string.check_adb), TvListings.adbGrant(LocalContext.current.packageName))
    }
}

@Composable
private fun WatchNextCard(item: WatchNextItem, size: CardSize, onOptions: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scale = if (item.shape == CardShape.POSTER) size.posterScale else size.scale
    val width = (if (item.shape == CardShape.POSTER) POSTER else WIDE) * scale
    val height = HEIGHT * scale
    val textStyle = size.titleStyle()
    val notOpened = stringResource(R.string.home_app_not_opened, item.title)
    Column(Modifier.width(width)) {
        TvCard(
            onClick = { context.openOrSay(notOpened, Toast.LENGTH_SHORT) { openWatchNext(item) } },
            onLongClick = onOptions,
            modifier = modifier.focusGlow(CardShape10, LocalAccent.current.color).size(width, height).semantics {
                contentDescription =
                    item.title
            },
            shape = CardShape10
        ) {
            Box(Modifier.fillMaxSize()) {
                Picture(item.pictureUri, width, height) {
                    Text(
                        item.title,
                        style = textStyle,
                        color = Palette.textDim,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                AppBadge(
                    item.packageName,
                    Modifier.align(Alignment.BottomStart).padding(start = 8.dp * size.scale, bottom = 12.dp * size.scale).size(
                        24.dp * size.scale
                    )
                )
                item.progress?.let { ProgressLine(it, Modifier.align(Alignment.BottomCenter), height = 4.dp) }
            }
        }
        Text(
            item.title,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        // The episode and the time left go in place of the episode's name, or any other subtitle (the owner, 2026-11-15:
        // they matter more), as on the sister apps' cards. Without them, the subtitle shows.
        (detail(item) ?: item.subtitle)?.let {
            if (size == CardSize.FULL) {
                Muted(it)
            } else {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** The season and episode with the time left, short ("S1E2 22m"), as the sister apps show them; null with neither. */
@Composable
private fun detail(item: WatchNextItem): String? {
    val season = item.season
    val episode = item.episode
    val code = if (season != null && episode != null) stringResource(R.string.home_episode_code, season, episode) else null
    val left = item.leftMs?.let { shortLeft(it) }
    return listOfNotNull(code, left).joinToString(" ").ifEmpty { null }
}

/** Time left, short, as a card has room for: "22m", "1h", "1h 5m" (whole minutes, rounded up; as MyTVPlayer's). */
@Composable
private fun shortLeft(ms: Long): String {
    val minutes = ((ms + MS_PER_MINUTE - 1) / MS_PER_MINUTE).toInt()
    return when {
        minutes < MINUTES_PER_HOUR -> stringResource(R.string.home_left_m, minutes)
        minutes % MINUTES_PER_HOUR == 0 -> stringResource(R.string.home_left_h, minutes / MINUTES_PER_HOUR)
        else -> stringResource(R.string.home_left_h_m, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    }
}

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60

/**
 * The item's picture, decoded at the card's size (not the app's full size), with [placeholder] until it loads, or if it
 * has none or fails (as MyPlexPlayer's ImageWithInitials). A picture about the card's shape is cropped to fill it, and
 * one of another shape (a TV channel's logo, the owner, 2026-11-15) is scaled to fit, whole (see [fitsWhole]).
 */
@Composable
private fun Picture(uri: String?, width: Dp, height: Dp, placeholder: @Composable () -> Unit) {
    var loaded by remember(uri) { mutableStateOf(false) }
    if (!loaded) placeholder()
    if (uri == null) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val request = remember(uri, width, height) {
        with(density) {
            ImageRequest.Builder(context).data(uri).size(width.roundToPx(), height.roundToPx()).allowRgb565(true).build()
        }
    }
    AsyncImage(
        request,
        contentDescription = null,
        contentScale = CropOrFit,
        onSuccess = { loaded = true },
        modifier = Modifier.fillMaxSize()
    )
}

/** The app's icon, small, on the card: which app the item opens in. */
@Composable
private fun AppBadge(packageName: String, modifier: Modifier) {
    val icon = rememberAppIcon(packageName)
    icon?.let { Image(it, contentDescription = null, modifier = modifier.clip(RoundedCornerShape(6.dp))) }
}

/** An item that the remote held OK on: a Continue watching item, or an item of the channel [channelId]. */
data class ItemChoice(val item: WatchNextItem, val channelId: Long? = null)

/**
 * An item's options: open its app, then for Continue watching hide the item or all of its app's items, and for a
 * channel hide the channel's row (Settings › Home rows undoes each).
 */
@Composable
fun ItemOptions(choice: ItemChoice, onDismiss: () -> Unit) {
    val item = choice.item
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
        val channel = choice.channelId
        if (channel != null) {
            BoardOption(stringResource(R.string.item_hide_channel), stringResource(R.string.item_hide_channel_help)) {
                onDismiss()
                container.appScope.launch { container.channels.setChannelShown(channel, false) }
            }
            return@BoardPanel
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

private val CardShape10 = RoundedCornerShape(10.dp)

/**
 * Whether a picture of [pictureAspect] (width over height) is shown whole on a card of [cardAspect], not cropped: when
 * cropping would cut off more than about a fifth of it.
 */
internal fun fitsWhole(pictureAspect: Float, cardAspect: Float): Boolean = (pictureAspect / cardAspect) !in CROP_RANGE

private val CROP_RANGE = 0.8f..1.25f

/** Crops a picture about the card's shape to fill it, and fits one of another shape whole, when the card is laid out. */
private object CropOrFit : ContentScale {
    override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
        val whole = srcSize.height > 0f && dstSize.height > 0f && fitsWhole(srcSize.width / srcSize.height, dstSize.width / dstSize.height)
        return (if (whole) ContentScale.Fit else ContentScale.Crop).computeScaleFactor(srcSize, dstSize)
    }
}

/**
 * The cards' size: Continue watching's, or the channel rows' smaller size, with smaller text. A poster in a channel row
 * is three quarters of Continue watching's, not half (the owner, 2026-11-15: half was too small), so it is taller than
 * the wide cards beside it.
 */
private enum class CardSize(val scale: Float, val posterScale: Float) {
    FULL(1f, 1f),
    HALF(0.5f, 0.75f);

    @Composable
    fun titleStyle(): TextStyle = if (this == FULL) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall
}

private val POSTER = 124.dp
private val WIDE = 331.dp
private val HEIGHT = 186.dp
