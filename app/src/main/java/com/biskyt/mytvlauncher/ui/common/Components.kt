package com.biskyt.mytvlauncher.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import com.biskyt.mytvlauncher.ui.theme.LocalOnAccentTypography
import com.biskyt.mytvlauncher.ui.theme.Palette

/* Copied from MyPlexPlayer's ui/common/Components.kt, keeping only what the launcher uses, so ports apply. */

/** The ring around what has focus. */
val FocusStroke = BorderStroke(3.dp, Palette.text)
private val FocusRing = Border(FocusStroke)

/** The ring inside a filled card that is in use (see TvCard's fill). A Border's inset is outwards: in, it is less than 0. */
private val SelectedRing = 2.dp
private val SelectedRingInset = (-3).dp

/** A card's shape. */
val CardShape = RoundedCornerShape(14.dp)

/**
 * The app's one clickable surface: neutral background, white ring when focused, optional selected state in accent.
 * With [fill] (settings rows and menus, the owner 2026-11-06), it is filled with the accent colour when focused, with
 * no ring, and its text takes the accent's text colour (see [LocalCardFilled]). Only a filled card that is [selected]
 * (the track, playlist or guide in use) has a ring then, inside its fill, so that it still shows which one is in use.
 * No focus scale animation by default — re-laying out rows on every focus change is costly on slow boxes.
 * [onLongClick] (holding OK) is also opened by the remote's Info/Options/Menu button. On a touch screen: tap and long press.
 */
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    shape: Shape = CardShape,
    color: Color = Palette.surface,
    focusedColor: Color = Palette.surfaceHigh,
    /** Without it, what holds the card shows its focus (the series page's episode rows). */
    ring: Boolean = true,
    fill: Boolean = false,
    /** Its background while [selected] and without focus. */
    selectedColor: Color = LocalAccent.current.tint,
    /** Its text's colour without focus (a panel's Delete is red). */
    contentColor: Color = Palette.text,
    content: @Composable BoxScope.() -> Unit
) {
    val accent = LocalAccent.current
    var focused by remember { mutableStateOf(false) }
    val focusFill = if (fill) {
        accent.color
    } else if (selected) {
        accent.tint
    } else {
        focusedColor
    }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        // Back on its screen, the card used last has focus again.
        modifier = (if (onLongClick != null) modifier.onOptionsKey(onLongClick) else modifier).returnsFocus()
            .onFocusChanged { focused = it.hasFocus }
            .onTap(onLongClick, onClick),
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) selectedColor else color,
            focusedContainerColor = focusFill,
            pressedContainerColor = if (fill) accent.color else focusedColor,
            contentColor = contentColor,
            focusedContentColor = if (fill) accent.onAccent else contentColor
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = when {
                // In use and filled: a ring in the fill's text colour, so it still shows which one is in use.
                fill && selected -> Border(BorderStroke(SelectedRing, accent.onAccent), SelectedRingInset)
                ring && !fill -> FocusRing
                else -> Border.None
            }
        )
    ) {
        val filled = fill && focused
        CompositionLocalProvider(LocalCardFocused provides focused, LocalCardFilled provides filled) {
            // Branched on fill (fixed for a card), not on focus: the same composition either way, so a focus move does
            // not dispose and recreate the content (its effects, such as a focus restore, would run again).
            val box = this
            if (fill) FilledCardText(filled) { box.content() } else content()
        }
    }
}

/**
 * The theme's text styles (they set the text colour themselves), in the accent's text colour while [filled], for text
 * on the accent fill. The theme makes those styles once (see [LocalOnAccentTypography]), not each card.
 */
@Composable
private fun FilledCardText(filled: Boolean, content: @Composable () -> Unit) {
    // Always through MaterialTheme, filled or not: the same composition either way, so a focus move does not dispose
    // and recreate the content.
    val t = MaterialTheme.typography
    MaterialTheme(MaterialTheme.colorScheme, MaterialTheme.shapes, if (filled) LocalOnAccentTypography.current else t, content)
}

/** Whether the [TvCard] around this content is filled with the accent colour now (focused, with fill). */
val LocalCardFilled = compositionLocalOf { false }

/**
 * A colour of its own for text or a mark in a card: [normal], or on the card's fill the accent's text colour (at
 * [onAccentAlpha], for something fainter).
 */
@Composable
fun cardColor(normal: Color, onAccentAlpha: Float = 1f): Color =
    if (LocalCardFilled.current) LocalAccent.current.onAccent.copy(alpha = onAccentAlpha) else normal

/** Whether the [TvCard] around this content has focus (e.g. for [OneLine]); only readers redraw when it changes. */
val LocalCardFocused = compositionLocalOf { false }

/**
 * Pill-shaped button. [primary] fills it with the accent colour; [selected] (a choice in use) with the text colour.
 * With focus, every button is filled with the accent colour, with no white ring (the owner, 2026-11-11 for the player,
 * 2026-11-13 for every button), and its text takes the accent's text colour. A primary or selected button has a thin
 * ring inside the fill then, so that focus still shows on it. [onLongClick] as for [TvCard].
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    val accent = LocalAccent.current
    val bg = when {
        primary -> accent.color
        selected -> Palette.text
        else -> Palette.surfaceHigh
    }
    val fg = when {
        primary -> accent.onAccent
        selected -> Palette.background
        else -> Palette.text
    }
    // Filled with the accent colour while it has focus, as every button is: a primary or selected one has the in-use ring.
    TvCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        selected = primary || selected,
        shape = RoundedCornerShape(50),
        color = bg,
        selectedColor = bg,
        fill = true,
        contentColor = fg
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Box(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = cardColor(fg),
                maxLines = 1
            )
        }
    }
}

/** Thin progress bar drawn in one pass (no nested layouts), with a round [marker] (e.g. while scrubbing) when given. */
@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, height: Dp = 5.dp, marker: Float? = null) {
    val accent = LocalAccent.current.color
    Box(
        modifier.height(height).fillMaxWidth().drawBehind {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Palette.track, cornerRadius = r)
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) drawRoundRect(accent, size = Size(size.width * f, size.height), cornerRadius = r)
            if (marker != null) {
                val center = Offset(size.width * marker.coerceIn(0f, 1f), size.height / 2)
                drawCircle(accent, radius = 11.dp.toPx(), center = center)
                drawCircle(Color.White, radius = 6.dp.toPx(), center = center)
            }
        }
    )
}

/**
 * One line of text: cut short with "…" normally, scrolling a few times to show the rest while its [TvCard] has focus
 * (only the focused item animates, and not for ever).
 */
@Composable
fun OneLine(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    focused: Boolean = LocalCardFocused.current
) {
    if (focused) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = modifier.basicMarquee()
        )
    } else {
        Text(text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = modifier.padding(bottom = 12.dp))
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = cardColor(Palette.textMuted, MUTED_ON_ACCENT),
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/** On/off switch drawn without animation. */
@Composable
fun Toggle(on: Boolean) {
    // On a card's accent fill: a track in the accent's text colour, and a knob in the accent colour.
    val accent = LocalAccent.current
    val track = if (on) cardColor(accent.color) else cardColor(Color(0xFF3A404C), TRACK_OFF_ON_ACCENT)
    Box(Modifier.size(52.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(track)) {
        Box(
            Modifier.padding(
                3.dp
            ).size(24.dp).align(if (on) Alignment.CenterEnd else Alignment.CenterStart).clip(CircleShape)
                .background(if (on && LocalCardFilled.current) accent.color else Palette.text)
        )
    }
}

/** A settings row: label + help text on the left, [trailing] control on the right. */
@Composable
fun SettingRow(
    label: String,
    help: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    TvCard(onClick = onClick, modifier = modifier.fillMaxWidth(), color = Palette.surfaceLow, fill = true) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                if (help != null) Muted(help, maxLines = 2)
            }
            trailing()
        }
    }
}

/** On a card's accent fill: help text a little lighter than the label, an off switch's track faint. */
private const val MUTED_ON_ACCENT = 0.78f
private const val TRACK_OFF_ON_ACCENT = 0.25f
