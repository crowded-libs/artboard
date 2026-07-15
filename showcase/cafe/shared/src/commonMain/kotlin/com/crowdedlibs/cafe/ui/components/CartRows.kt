package com.crowdedlibs.cafe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.CartLine
import com.crowdedlibs.cafe.model.CartTotals
import com.crowdedlibs.cafe.model.formatPrice
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.cart_discount
import com.crowdedlibs.cafe.resources.cart_promo_applied
import com.crowdedlibs.cafe.resources.cart_promo_apply
import com.crowdedlibs.cafe.resources.cart_promo_hint
import com.crowdedlibs.cafe.resources.cart_promo_invalid
import com.crowdedlibs.cafe.resources.cart_subtotal
import com.crowdedlibs.cafe.resources.cart_tax
import com.crowdedlibs.cafe.resources.cart_total
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** One line on the order ticket: cup mark, name, size·unit price, stepper, total. */
@Composable
fun CartLineRow(
    line: CartLine,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Monogram(line.item.name, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.item.name, style = type.serifItem.copy(color = colors.ink))
            Text(
                "${line.size.short} · ${formatPrice(line.unitPriceCents)}",
                style = type.mono.copy(color = colors.inkSoft),
            )
        }
        QuantityStepper(quantity = line.quantity, onQuantityChange = onQuantityChange)
        Text(formatPrice(line.totalCents), style = type.price.copy(color = colors.ink))
    }
}

/**
 * The receipt tally: mono rows with dot leaders, a dashed tear line, and the
 * total set larger. Reads like the slip the register prints.
 */
@Composable
fun TotalsSummary(
    totals: CartTotals,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LeaderRow(stringResource(Res.string.cart_subtotal), formatPrice(totals.subtotalCents))
        if (totals.discountCents > 0) {
            LeaderRow(stringResource(Res.string.cart_discount), "−" + formatPrice(totals.discountCents), accent = true)
        }
        LeaderRow(stringResource(Res.string.cart_tax), formatPrice(totals.taxCents))
        DashedRule(Modifier.fillMaxWidth().padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(Res.string.cart_total),
                style = type.label.copy(color = colors.ink),
                modifier = Modifier.weight(1f),
            )
            Text(
                formatPrice(totals.totalCents),
                style = type.price.copy(color = colors.ink, fontSize = 20.sp),
            )
        }
    }
}

@Composable
private fun LeaderRow(label: String, amount: String, accent: Boolean = false) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    val amountColor = if (accent) colors.stamp else colors.ink
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(label, style = type.mono.copy(color = colors.inkSoft))
        DashedRule(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp))
        Text(amount, style = type.mono.copy(color = amountColor))
    }
}

/** Promo entry; the demo code is CREMA. */
@Composable
fun PromoCodeField(
    applied: Boolean,
    onApply: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    var code by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = code,
                onValueChange = { code = it },
                textStyle = type.mono.copy(color = colors.ink),
                singleLine = true,
                cursorBrush = SolidColor(colors.stamp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(
                        Modifier.hairline().padding(horizontal = 14.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (code.isEmpty()) {
                            Text(stringResource(Res.string.cart_promo_hint), style = type.mono.copy(color = colors.inkSoft))
                        }
                        inner()
                    }
                },
            )
            TicketButton(
                text = stringResource(Res.string.cart_promo_apply),
                onClick = { invalid = !onApply(code) },
            )
        }
        if (applied) {
            Text(
                stringResource(Res.string.cart_promo_applied),
                style = type.mono.copy(color = colors.brew),
            )
        } else if (invalid) {
            Text(
                stringResource(Res.string.cart_promo_invalid),
                style = type.mono.copy(color = colors.stamp),
            )
        }
    }
}

@Preview(name = "Cart line", group = "Cart", widthDp = 380)
@Composable
fun CartLineRowPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CartLineRow(line = SampleData.sampleCart.first(), onQuantityChange = {}, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "Totals receipt", group = "Cart", widthDp = 320)
@Composable
fun TotalsSummaryPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        TotalsSummary(
            totals = CartTotals(subtotalCents = 1725, discountCents = 173, taxCents = 140),
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(name = "Promo code · applied", group = "Cart", widthDp = 360)
@Composable
fun PromoCodeFieldPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        PromoCodeField(applied = true, onApply = { true }, modifier = Modifier.padding(12.dp))
    }
}
