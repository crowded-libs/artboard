package com.crowdedlibs.cafe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.ui.theme.CafeTheme

/**
 * A torn thermal-receipt edge: a rectangle whose [top] and/or [bottom] edges
 * are a row of small triangular perforations, as if ripped off the printer.
 * The tooth pitch is fixed in px so tickets of any width read as the same paper.
 */
class TicketShape(
    private val toothWidthPx: Float = 22f,
    private val toothDepthPx: Float = 7f,
    private val top: Boolean = false,
    private val bottom: Boolean = true,
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val teeth = (w / toothWidthPx).toInt().coerceAtLeast(1)
        val step = w / teeth

        // Top edge: flat, or a row of downward notches.
        if (top) {
            path.moveTo(0f, toothDepthPx)
            var x = 0f
            for (i in 0 until teeth) {
                path.lineTo(x + step / 2f, 0f)
                path.lineTo(x + step, toothDepthPx)
                x += step
            }
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(w, 0f)
        }

        // Down the right side to the bottom edge.
        path.lineTo(w, if (bottom) h - toothDepthPx else h)

        if (bottom) {
            var x = w
            for (i in 0 until teeth) {
                path.lineTo(x - step / 2f, h)
                path.lineTo(x - step, h - toothDepthPx)
                x -= step
            }
        } else {
            path.lineTo(0f, h)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * The workhorse surface: flat cream ticket stock with a hairline ink border and
 * near-square corners. No Material elevation — paper doesn't cast shadows on a
 * counter. Pass a [TicketShape] via [torn] for receipt edges.
 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    color: Color? = null,
    border: Boolean = true,
    corner: Dp = 3.dp,
    torn: TicketShape? = null,
    content: @Composable () -> Unit,
) {
    val colors = CafeTheme.colors
    val shape: Shape = torn ?: RoundedCornerShape(corner)
    var m = modifier.clip(shape).background(color ?: colors.ticket, shape)
    if (border && torn == null) {
        m = m.border(1.dp, colors.line, shape)
    }
    Box(m) { content() }
}

/**
 * A dashed receipt rule — the dotted line you tear a total off along. Drawn, not
 * a stock divider, because the dash is the point.
 */
@Composable
fun DashedRule(
    modifier: Modifier = Modifier,
    color: Color = CafeTheme.colors.line,
) {
    Canvas(modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
        )
    }
}

/**
 * A rubber-stamp mark: tracked-out mono label inside a rotated, outlined box, as
 * if pressed onto the ticket with an ink stamp. Used for "Featured" and "Sold out".
 */
@Composable
fun StampMark(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CafeTheme.colors.stamp,
    rotate: Float = -4f,
) {
    val type = CafeTheme.type
    Box(
        modifier
            .graphicsLayer { rotationZ = rotate }
            .clip(RoundedCornerShape(2.dp))
            .border(1.5.dp, color, RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = type.label.copy(color = color),
        )
    }
}

/** Semantic hairline-border modifier for paper regions that aren't full cards. */
@Composable
fun Modifier.hairline(corner: Dp = 3.dp): Modifier =
    this.border(1.dp, CafeTheme.colors.line, RoundedCornerShape(corner))

/** Quiet secondary action: an outlined ticket with a tracked mono label. */
@Composable
fun TicketButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, colors.ink, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(text, style = type.label.copy(color = colors.ink))
    }
}

/** The one loud action: a full stamp-orange bar. Reserve for a screen's primary verb. */
@Composable
fun StampButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    val fill = if (enabled) colors.stamp else colors.line
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(fill)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(vertical = 16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(text, style = type.label.copy(color = colors.ticket))
    }
}
