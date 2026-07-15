package com.crowdedlibs.cafe.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.ui.theme.CafeTheme

/**
 * The chalkboard hero at the top of the menu: the café name lettered big in the
 * display serif on the board green, with the day's line beneath. This is the
 * screen's thesis — the first and most characteristic thing you see.
 */
@Composable
fun BoardHero(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.board)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(eyebrow.uppercase(), style = type.label.copy(color = colors.stamp))
        Text(title, style = type.board.copy(color = colors.chalk))
        Text(subtitle, style = type.body.copy(color = colors.chalk.copy(alpha = 0.72f)))
    }
}

/** A serif screen heading over a tracked eyebrow, with optional trailing content. */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(eyebrow.uppercase(), style = type.label.copy(color = colors.stamp))
            Text(title, style = type.heading.copy(color = colors.ink))
        }
        if (trailing != null) trailing()
    }
}

/** A quiet "‹ back" link in the receipt monospace. */
@Composable
fun BackLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Text(
        text = "‹ " + text.uppercase(),
        style = type.label.copy(color = colors.inkSoft),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

/**
 * "Ink settling": on first composition, fade and lift each item into place with
 * a per-[index] stagger, so the board reads top-to-bottom like it's being
 * written up. Honors [CafeTheme.reduceMotion] by rendering statically.
 */
@Composable
fun SettleIn(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (CafeTheme.reduceMotion) {
        Box(modifier) { content() }
        return
    }
    val progress = remember { Animatable(0f) }
    val liftPx = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 340, delayMillis = index * 55),
        )
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * liftPx
        },
    ) { content() }
}
