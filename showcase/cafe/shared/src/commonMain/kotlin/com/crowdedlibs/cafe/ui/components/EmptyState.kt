package com.crowdedlibs.cafe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.cart_browse_menu
import com.crowdedlibs.cafe.resources.cart_empty_body
import com.crowdedlibs.cafe.resources.cart_empty_title
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Centred empty/error state. A single drawn cup-ring stands in for the missing
 * thing, then a serif line of direction and an optional action. [glyph] is the
 * character pressed into the ring — kept ASCII ("+", "!", "·") so the wasm
 * gallery host renders it without an emoji font.
 */
@Composable
fun EmptyState(
    glyph: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(
        modifier = modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(72.dp)) {
                val r = size.minDimension / 2f
                val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(colors.line, radius = r - 1.5f, center = c, style = Stroke(width = 1.5f))
                drawCircle(colors.stamp, radius = r * 0.66f, center = c, style = Stroke(width = 2.5f))
            }
            Text(glyph, style = type.heading.copy(color = colors.stamp))
        }
        Text(title, style = type.heading.copy(color = colors.ink), textAlign = TextAlign.Center)
        Text(
            body,
            style = type.body.copy(color = colors.inkSoft),
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            StampButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** Static skeleton ticket rows for the menu's loading phase (wasm-safe, no timers). */
@Composable
fun LoadingListPlaceholder(
    rows: Int = 4,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(rows) { i ->
            PaperCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)).background(colors.line.copy(alpha = 0.5f)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Bar(fraction = 0.5f)
                        Bar(fraction = if (i % 2 == 0) 0.85f else 0.7f, thin = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, thin: Boolean = false) {
    val colors = CafeTheme.colors
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(if (thin) 9.dp else 13.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.line.copy(alpha = if (thin) 0.4f else 0.6f)),
    )
}

@Preview(name = "Empty cart", group = "Empty states", widthDp = 300, heightDp = 340)
@Composable
fun EmptyCartStatePreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        Box(Modifier.background(CafeTheme.colors.paper)) {
            EmptyState(
                glyph = "+",
                title = stringResource(Res.string.cart_empty_title),
                body = stringResource(Res.string.cart_empty_body),
                actionLabel = stringResource(Res.string.cart_browse_menu),
            )
        }
    }
}

@Preview(name = "Loading skeleton", group = "Empty states", widthDp = 360)
@Composable
fun LoadingListPlaceholderPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        Box(Modifier.background(CafeTheme.colors.paper).padding(16.dp)) {
            LoadingListPlaceholder()
        }
    }
}
