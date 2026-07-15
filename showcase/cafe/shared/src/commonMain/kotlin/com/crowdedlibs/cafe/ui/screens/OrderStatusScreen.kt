package com.crowdedlibs.cafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.Order
import com.crowdedlibs.cafe.model.formatPrice
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.app_name
import com.crowdedlibs.cafe.resources.order_advance_demo
import com.crowdedlibs.cafe.resources.order_error_body
import com.crowdedlibs.cafe.resources.order_error_title
import com.crowdedlibs.cafe.resources.order_eyebrow
import com.crowdedlibs.cafe.resources.order_none_body
import com.crowdedlibs.cafe.resources.order_none_title
import com.crowdedlibs.cafe.resources.order_pickup_code
import com.crowdedlibs.cafe.resources.order_retry
import com.crowdedlibs.cafe.resources.order_thanks
import com.crowdedlibs.cafe.resources.order_title
import com.crowdedlibs.cafe.ui.CafeAppFrame
import com.crowdedlibs.cafe.ui.CafeTab
import com.crowdedlibs.cafe.ui.components.DashedRule
import com.crowdedlibs.cafe.ui.components.EmptyState
import com.crowdedlibs.cafe.ui.components.OrderStatusTimeline
import com.crowdedlibs.cafe.ui.components.PaperCard
import com.crowdedlibs.cafe.ui.components.ScreenHeader
import com.crowdedlibs.cafe.ui.components.StatusBadge
import com.crowdedlibs.cafe.ui.components.Text
import com.crowdedlibs.cafe.ui.components.TicketButton
import com.crowdedlibs.cafe.ui.components.TicketShape
import com.crowdedlibs.cafe.ui.label
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** Orders tab: the printed pickup receipt and its live status, or empty/error. */
@Composable
fun OrderStatusScreen(
    order: Order?,
    hasError: Boolean,
    onRetry: () -> Unit,
    onAdvanceDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    Column(modifier.fillMaxSize().background(colors.paper)) {
        when {
            hasError -> EmptyState(
                glyph = "!",
                title = stringResource(Res.string.order_error_title),
                body = stringResource(Res.string.order_error_body),
                actionLabel = stringResource(Res.string.order_retry),
                onAction = onRetry,
                modifier = Modifier.padding(top = 72.dp),
            )

            order == null -> EmptyState(
                glyph = "·",
                title = stringResource(Res.string.order_none_title),
                body = stringResource(Res.string.order_none_body),
                modifier = Modifier.padding(top = 72.dp),
            )

            else -> Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ScreenHeader(
                    eyebrow = stringResource(Res.string.order_eyebrow),
                    title = stringResource(Res.string.order_title),
                    trailing = { StatusBadge(order.status) },
                )
                Receipt(order)
                OrderStatusTimeline(current = order.status)
                TicketButton(
                    text = stringResource(Res.string.order_advance_demo),
                    onClick = onAdvanceDemo,
                )
            }
        }
    }
}

/** The thermal receipt: torn edges, dashed rules, the pickup code stamped big. */
@Composable
private fun Receipt(order: Order) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    PaperCard(
        modifier = Modifier.fillMaxWidth(),
        torn = TicketShape(top = true, bottom = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.app_name).uppercase(),
                style = type.label.copy(color = colors.ink, letterSpacing = 3.sp),
            )
            Text("No. ${order.id.substringAfter('-')}", style = type.mono.copy(color = colors.inkSoft))
            DashedRule(Modifier.fillMaxWidth())
            Text(
                stringResource(Res.string.order_pickup_code).uppercase(),
                style = type.label.copy(color = colors.inkSoft),
            )
            Text(order.pickupCode, style = type.code.copy(color = colors.stamp))
            DashedRule(Modifier.fillMaxWidth())
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                order.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text("${line.quantity}× ${line.item.name}", style = type.mono.copy(color = colors.ink))
                        DashedRule(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp))
                        Text(formatPrice(line.totalCents), style = type.mono.copy(color = colors.ink))
                    }
                }
            }
            DashedRule(Modifier.fillMaxWidth())
            Text(
                stringResource(Res.string.order_thanks).uppercase(),
                style = type.label.copy(color = colors.inkSoft),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Order status · preparing", group = "Orders", widthDp = 402, heightDp = 874)
@Composable
fun OrderStatusScreenPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Orders, cartCount = 0) { padding ->
            OrderStatusScreen(
                order = SampleData.sampleOrder,
                hasError = false,
                onRetry = {},
                onAdvanceDemo = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "Order status · error", group = "Orders", widthDp = 402, heightDp = 874)
@Composable
fun OrderStatusScreenErrorPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Orders, cartCount = 0) { padding ->
            OrderStatusScreen(
                order = null,
                hasError = true,
                onRetry = {},
                onAdvanceDemo = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}
