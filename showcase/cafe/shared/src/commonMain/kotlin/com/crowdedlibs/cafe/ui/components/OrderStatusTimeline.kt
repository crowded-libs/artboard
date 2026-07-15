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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.model.OrderStatus
import com.crowdedlibs.cafe.ui.label
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Progress through the order lifecycle, drawn as stamped checkpoints down a
 * connecting rule: reached steps are solid stamp dots, the rest hollow rings
 * (the cup mark, not yet pressed). The current step is set in the display serif.
 */
@Composable
fun OrderStatusTimeline(
    current: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(modifier = modifier.fillMaxWidth()) {
        OrderStatus.entries.forEachIndexed { index, status ->
            val reached = status.ordinal <= current.ordinal
            val isCurrent = status == current
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val r = size.minDimension / 2f
                    val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    if (reached) {
                        drawCircle(colors.stamp, radius = r, center = c)
                    } else {
                        drawCircle(colors.line, radius = r - 1f, center = c, style = Stroke(width = 2f))
                    }
                }
                Text(
                    status.label(),
                    style = if (isCurrent) {
                        type.serifItem.copy(color = colors.ink)
                    } else {
                        type.body.copy(color = if (reached) colors.ink else colors.inkSoft)
                    },
                )
            }
            if (index < OrderStatus.entries.lastIndex) {
                Box(
                    Modifier
                        .padding(start = 7.dp)
                        .size(width = 2.dp, height = 18.dp)
                        .background(if (status.ordinal < current.ordinal) colors.stamp else colors.line),
                )
            }
        }
    }
}

/** Pill badge for a single order status. */
@Composable
fun StatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    val (fill, ink) = when (status) {
        OrderStatus.Ready, OrderStatus.PickedUp -> colors.brew to colors.ticket
        else -> colors.stamp to colors.ticket
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(status.label(), style = type.label.copy(color = ink))
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Timeline · preparing", group = "Status", widthDp = 340)
@Composable
fun OrderStatusTimelinePreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        Box(Modifier.background(CafeTheme.colors.paper).padding(16.dp)) {
            OrderStatusTimeline(current = OrderStatus.Preparing)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Status badges", group = "Status")
@Composable
fun StatusBadgePreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        Column(
            modifier = Modifier.background(CafeTheme.colors.paper).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderStatus.entries.forEach { StatusBadge(it) }
        }
    }
}
