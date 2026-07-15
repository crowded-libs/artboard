package com.crowdedlibs.cafe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crowdedlibs.cafe.ui.theme.CafeTheme

/**
 * Where product photography would go, the café stamps a coffee-cup ring and
 * writes the item's initial inside it — the mark a barista leaves on a ticket.
 * Two concentric hand-weight rings (the stain a warm cup leaves) around a mono
 * letter. Deliberately drawn, not an emoji or `Icons.Default.*`: it renders
 * identically on the wasm gallery host, which has no icon or emoji font.
 *
 */
@Composable
fun Monogram(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    style: TextStyle? = null,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    val letterStyle = (style ?: type.price.copy(fontSize = (size.value * 0.34f).sp))
        .copy(color = colors.stamp)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            // Outer stain ring, then a tighter inner ring — a cup set down twice.
            drawCircle(colors.stamp, radius = r - 1.5f, center = c, style = Stroke(width = 1.5f))
            drawCircle(colors.stamp, radius = r * 0.72f, center = c, style = Stroke(width = 3f))
        }
        Text(text.firstOrNull()?.uppercase() ?: "·", style = letterStyle)
    }
}
