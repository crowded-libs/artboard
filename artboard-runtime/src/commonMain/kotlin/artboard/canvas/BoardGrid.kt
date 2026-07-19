package artboard.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Square graph-paper grid for the **board canvas** (full panel background).
 * Not the same as [ColumnLayoutGrid] used on individual screens.
 *
 * @param scale current camera zoom; minor lines fade out below 0.6 and vanish
 *   below 0.35 so the paper stays quiet when zoomed far out (grid is drawn in
 *   screen space, so only opacity responds to zoom).
 */
@Composable
fun DesignSystemGrid(
    modifier: Modifier = Modifier,
    minorStep: Dp = 8.dp,
    majorEvery: Int = 8,
    minorColor: Color = Color.Black.copy(alpha = 0.06f),
    majorColor: Color = Color.Black.copy(alpha = 0.12f),
    scale: Float = 1f,
) {
    val density = LocalDensity.current
    val stepPx = with(density) { minorStep.toPx() }.coerceAtLeast(1f)
    val minorFade = ((scale - 0.35f) / 0.25f).coerceIn(0f, 1f)
    val fadedMinor = minorColor.copy(alpha = minorColor.alpha * minorFade)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        var i = 0
        var x = 0f
        while (x <= w + 0.5f) {
            val major = i % majorEvery == 0
            if (major || minorFade > 0f) {
                drawLine(
                    color = if (major) majorColor else fadedMinor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = if (major) 1.25f else 0.75f,
                )
            }
            x += stepPx
            i++
        }
        i = 0
        var y = 0f
        while (y <= h + 0.5f) {
            val major = i % majorEvery == 0
            if (major || minorFade > 0f) {
                drawLine(
                    color = if (major) majorColor else fadedMinor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (major) 1.25f else 0.75f,
                )
            }
            y += stepPx
            i++
        }
    }
}

/**
 * Material-style **column layout grid** for screen previews: outer margins,
 * vertical columns, and gutters — semi-transparent bands so you can check that
 * content aligns to the design system (not a square graph paper).
 *
 * Visual model: phone layout grid (margin | col | gutter | col | … | margin).
 */
@Composable
fun ColumnLayoutGrid(
    modifier: Modifier = Modifier,
    columns: Int = 6,
    margin: Dp = 16.dp,
    gutter: Dp = 8.dp,
    // Non-photo blue by default (drafting pencil that repro cameras couldn't see).
    columnColor: Color = Color(0xFF62A8DC).copy(alpha = 0.30f),
) {
    val density = LocalDensity.current
    val marginPx = with(density) { margin.toPx() }
    val gutterPx = with(density) { gutter.toPx() }
    val colCount = columns.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val inner = (w - marginPx * 2f - gutterPx * (colCount - 1)).coerceAtLeast(0f)
        val colW = inner / colCount
        if (colW <= 0f) return@Canvas

        var x = marginPx
        repeat(colCount) {
            drawRect(
                color = columnColor,
                topLeft = Offset(x, 0f),
                size = Size(colW, h),
            )
            x += colW + gutterPx
        }
    }
}
