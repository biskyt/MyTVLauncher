package com.biskyt.mytvlauncher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The launcher's highlight for the card with focus (the owner, 2026-11-15: "fancier" than the ring alone): the card
 * grows a little and glows in [color] (the accent). Both are properties of the card's own drawing layer (its scale and
 * its shadow), which the GPU draws: a focus change animates the layer only, with no composition and no new layout, so
 * rows scroll without stutter. Put it on the card ([shape] is the card's), outside its focus target, so that it sees
 * the card's focus.
 */
fun Modifier.focusGlow(shape: Shape, color: Color): Modifier = this then FocusGlowElement(shape, color)

private data class FocusGlowElement(val shape: Shape, val color: Color) : ModifierNodeElement<FocusGlowNode>() {
    override fun create() = FocusGlowNode(shape, color)

    override fun update(node: FocusGlowNode) {
        node.shape = shape
        node.color = color
        // The layer reads them while it draws: place it again, so that it draws with the new ones.
        node.invalidatePlacement()
    }
}

private class FocusGlowNode(var shape: Shape, var color: Color) :
    Modifier.Node(),
    LayoutModifierNode,
    FocusEventModifierNode {
    private val lift = Animatable(0f)
    private var focused = false

    /** Read by the layer while it draws: a change of [lift] draws the layer again, and nothing else. */
    private val layer: GraphicsLayerScope.() -> Unit = {
        val amount = lift.value
        val scale = 1f + LIFT_SCALE * amount
        scaleX = scale
        scaleY = scale
        shadowElevation = GLOW_ELEVATION.toPx() * amount
        shape = this@FocusGlowNode.shape
        ambientShadowColor = color
        spotShadowColor = color
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.hasFocus == focused) return
        focused = focusState.hasFocus
        val target = if (focused) 1f else 0f
        coroutineScope.launch { lift.animateTo(target, tween(LIFT_MS)) }
    }
}

/** How much the card with focus grows: 6%, enough to see, little enough to stay inside the gap to the next card. */
private const val LIFT_SCALE = 0.06f

private const val LIFT_MS = 150

private val GLOW_ELEVATION = 16.dp

/** Room above and below a row of cards for the card with focus to grow and glow into. */
val GLOW_ROOM = 8.dp
