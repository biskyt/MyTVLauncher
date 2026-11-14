package com.biskyt.mytvlauncher.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The handful of icons the launcher needs, drawn as vectors (as MyPlexPlayer's AppIcons: no icon library). */
object LauncherIcons {
    private fun icon(name: String, block: PathBuilder.() -> Unit) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, pathBuilder = block)
    }.build()

    /** Android settings: MyPlexPlayer's gear. */
    val Settings = icon("settings") {
        val teeth = 8
        val step = 2 * PI / teeth
        fun point(angle: Double, r: Float) = (12 + r * cos(angle)).toFloat() to (12 + r * sin(angle)).toFloat()
        for (i in 0 until teeth) {
            val a = i * step
            // Root, up the tooth's side, across its top, down the other side.
            val corners = listOf(a - step * 0.32 to 7.2f, a - step * 0.18 to 9.6f, a + step * 0.18 to 9.6f, a + step * 0.32 to 7.2f)
            corners.forEachIndexed { k, (angle, r) ->
                val (x, y) = point(angle, r)
                if (i == 0 && k == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        close()
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, true, true, 9f, 12f)
        arcTo(3f, 3f, 0f, true, true, 15f, 12f)
    }

    /** The launcher's own settings: two sliders. */
    val Tune = icon("tune") {
        moveTo(4f, 7f)
        horizontalLineTo(13f)
        moveTo(19f, 7f)
        horizontalLineTo(20f)
        moveTo(18f, 7f)
        arcTo(2f, 2f, 0f, true, true, 14f, 7f)
        arcTo(2f, 2f, 0f, true, true, 18f, 7f)
        moveTo(4f, 17f)
        horizontalLineTo(5f)
        moveTo(11f, 17f)
        horizontalLineTo(20f)
        moveTo(10f, 17f)
        arcTo(2f, 2f, 0f, true, true, 6f, 17f)
        arcTo(2f, 2f, 0f, true, true, 10f, 17f)
    }

    /** All apps: four rounded squares. */
    val Grid = icon("grid") {
        listOf(3f to 3f, 14f to 3f, 3f to 14f, 14f to 14f).forEach { (x, y) ->
            moveTo(x + 1.5f, y)
            horizontalLineTo(x + 5.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, x + 7f, y + 1.5f)
            verticalLineTo(y + 5.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, x + 5.5f, y + 7f)
            horizontalLineTo(x + 1.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, x, y + 5.5f)
            verticalLineTo(y + 1.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, x + 1.5f, y)
            close()
        }
    }
}
