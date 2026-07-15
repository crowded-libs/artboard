package com.crowdedlibs.cafe.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.CartLine
import com.crowdedlibs.cafe.model.CartTotals
import com.crowdedlibs.cafe.model.formatPrice
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.checkout_eyebrow
import com.crowdedlibs.cafe.resources.checkout_pay_card
import com.crowdedlibs.cafe.resources.checkout_pay_counter
import com.crowdedlibs.cafe.resources.checkout_payment
import com.crowdedlibs.cafe.resources.checkout_pickup
import com.crowdedlibs.cafe.resources.checkout_pickup_15
import com.crowdedlibs.cafe.resources.checkout_pickup_30
import com.crowdedlibs.cafe.resources.checkout_pickup_asap
import com.crowdedlibs.cafe.resources.checkout_place_order
import com.crowdedlibs.cafe.resources.checkout_title
import com.crowdedlibs.cafe.ui.CafeAppFrame
import com.crowdedlibs.cafe.ui.CafeTab
import com.crowdedlibs.cafe.ui.components.BackLink
import com.crowdedlibs.cafe.ui.components.DashedRule
import com.crowdedlibs.cafe.ui.components.ScreenHeader
import com.crowdedlibs.cafe.ui.components.StampButton
import com.crowdedlibs.cafe.ui.components.Text
import com.crowdedlibs.cafe.ui.components.TotalsSummary
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** Final review: order summary, pickup slot, payment method. */
@Composable
fun CheckoutScreen(
    lines: List<CartLine>,
    totals: CartTotals,
    onPlaceOrder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    var pickupOption by remember { mutableIntStateOf(0) }
    var paymentOption by remember { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize().background(colors.paper).padding(20.dp)) {
        BackLink(text = stringResource(Res.string.checkout_title), onClick = onBack)
        ScreenHeader(
            eyebrow = stringResource(Res.string.checkout_eyebrow),
            title = stringResource(Res.string.checkout_title),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${line.quantity}× ${line.item.name} (${line.size.short})",
                            style = type.mono.copy(color = colors.ink),
                        )
                        DashedRule(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp))
                        Text(formatPrice(line.totalCents), style = type.mono.copy(color = colors.ink))
                    }
                }
            }

            OptionGroup(
                title = stringResource(Res.string.checkout_pickup),
                options = listOf(
                    stringResource(Res.string.checkout_pickup_asap),
                    stringResource(Res.string.checkout_pickup_15),
                    stringResource(Res.string.checkout_pickup_30),
                ),
                selected = pickupOption,
                onSelected = { pickupOption = it },
            )

            OptionGroup(
                title = stringResource(Res.string.checkout_payment),
                options = listOf(
                    stringResource(Res.string.checkout_pay_counter),
                    stringResource(Res.string.checkout_pay_card),
                ),
                selected = paymentOption,
                onSelected = { paymentOption = it },
            )

            TotalsSummary(totals = totals)
        }
        StampButton(
            text = stringResource(Res.string.checkout_place_order),
            onClick = onPlaceOrder,
            enabled = lines.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}

@Composable
private fun OptionGroup(
    title: String,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = type.label.copy(color = colors.inkSoft))
        options.forEachIndexed { index, label ->
            val chosen = index == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = chosen,
                        role = Role.RadioButton,
                        onClick = { onSelected(index) },
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val r = size.minDimension / 2f
                    val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    drawCircle(if (chosen) colors.stamp else colors.line, radius = r - 1f, center = c, style = Stroke(width = 2f))
                    if (chosen) drawCircle(colors.stamp, radius = r * 0.45f, center = c)
                }
                Text(
                    label,
                    style = type.body.copy(color = colors.ink),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Preview(name = "Checkout", group = "Ordering", widthDp = 402, heightDp = 874)
@Composable
fun CheckoutScreenPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Cart, cartCount = 4) { padding ->
            CheckoutScreen(
                lines = SampleData.sampleCart,
                totals = SampleData.sampleTotals,
                onPlaceOrder = {},
                onBack = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}
