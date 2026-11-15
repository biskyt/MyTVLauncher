package com.biskyt.mytvlauncher.ui.home

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The launcher's highlight for the card with focus (the owner, 2026-11-15: larger, with a glowing edge in place of a
 * solid ring): the card grows by a tenth and a soft glow in [color] (the accent) shows around its edge. The growth is a
 * property of the card's own drawing layer, and the glow one blurred shape drawn under it, both on the GPU: a focus
 * change animates them only, with no composition and no new layout, so rows scroll without stutter. Put it on the card
 * ([shape] is the card's), outside its focus target, so that it sees the card's focus. Put [raiseOnFocus] on the row's
 * item, so that the card draws over the cards next to it.
 */
fun Modifier.focusGlow(shape: Shape, color: Color): Modifier = this then FocusGlowElement(shape, color)

/** Draws the row's item with focus over the others, so that its card grows and glows over theirs (see [focusGlow]). */
fun Modifier.raiseOnFocus(): Modifier = this then RaiseElement

private data class FocusGlowElement(val shape: Shape, val color: Color) : ModifierNodeElement<FocusGlowNode>() {
    override fun create() = FocusGlowNode(shape, color)

    override fun update(node: FocusGlowNode) {
        node.shape = shape
        node.color = color
        // The glow reads them while it draws: draw it again, with the new ones.
        node.invalidateDraw()
    }
}

private class FocusGlowNode(var shape: Shape, var color: Color) :
    Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    FocusEventModifierNode {
    private val lift = Animatable(0f)
    private var focused = false
    private val paint = Paint()
    private var blurPx = 0f

    /** The card's outline, made again only when its size or shape changes, not on each frame of the animation. */
    private var outline: Outline? = null
    private var outlineSize = Size.Unspecified
    private var outlineShape: Shape? = null

    /** Read by the layer while it draws: a change of [lift] draws the layer again, and nothing else. */
    private val layer: GraphicsLayerScope.() -> Unit = {
        val scale = 1f + LIFT_SCALE * lift.value
        scaleX = scale
        scaleY = scale
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }

    /** The glow, under the card and grown with it: the card's shape, blurred outwards only. */
    override fun ContentDrawScope.draw() {
        val amount = lift.value
        if (amount > 0f) {
            val radius = GLOW_RADIUS.toPx()
            val framework = paint.asFrameworkPaint()
            if (radius != blurPx) {
                blurPx = radius
                framework.isAntiAlias = true
                framework.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.OUTER)
            }
            framework.color = color.copy(alpha = GLOW_ALPHA * amount).toArgb()
            if (outline == null || outlineSize != size || outlineShape != shape) {
                outline = shape.createOutline(size, layoutDirection, this)
                outlineSize = size
                outlineShape = shape
            }
            val edge = outline ?: return drawContent()
            val grown = 1f + LIFT_SCALE * amount
            scale(grown) { drawIntoCanvas { it.drawOutline(edge, paint) } }
        }
        drawContent()
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.hasFocus == focused) return
        focused = focusState.hasFocus
        val target = if (focused) 1f else 0f
        coroutineScope.launch { lift.animateTo(target, tween(LIFT_MS)) }
    }
}

private data object RaiseElement : ModifierNodeElement<RaiseNode>() {
    override fun create() = RaiseNode()

    override fun update(node: RaiseNode) = Unit
}

private class RaiseNode :
    Modifier.Node(),
    LayoutModifierNode,
    FocusEventModifierNode {
    private var focused = false

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0, zIndex = if (focused) 1f else 0f) }
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.hasFocus == focused) return
        focused = focusState.hasFocus
        invalidatePlacement()
    }
}

/** How much the card with focus grows: a tenth (the owner, 2026-11-15: larger than the 6% before). */
private const val LIFT_SCALE = 0.1f

private const val LIFT_MS = 150

/** How far the glow reaches out from the card's edge, and how strong it is at the edge. */
private val GLOW_RADIUS = 12.dp
private const val GLOW_ALPHA = 0.85f

/** Room above and below a row of cards for the card with focus to grow and glow into. */
val GLOW_ROOM = 20.dp

/** The gap under a full size card to its title, clear of the card as it grows (half its growth, and a little more). */
val GLOW_GAP = 12.dp
