package com.crowdedlibs.cafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.CartLine
import com.crowdedlibs.cafe.model.CartTotals
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.cart_browse_menu
import com.crowdedlibs.cafe.resources.cart_checkout
import com.crowdedlibs.cafe.resources.cart_empty_body
import com.crowdedlibs.cafe.resources.cart_empty_title
import com.crowdedlibs.cafe.resources.cart_eyebrow
import com.crowdedlibs.cafe.resources.cart_title
import com.crowdedlibs.cafe.ui.CafeAppFrame
import com.crowdedlibs.cafe.ui.CafeTab
import com.crowdedlibs.cafe.ui.components.CartLineRow
import com.crowdedlibs.cafe.ui.components.DashedRule
import com.crowdedlibs.cafe.ui.components.EmptyState
import com.crowdedlibs.cafe.ui.components.PromoCodeField
import com.crowdedlibs.cafe.ui.components.ScreenHeader
import com.crowdedlibs.cafe.ui.components.StampButton
import com.crowdedlibs.cafe.ui.components.TotalsSummary
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** Cart tab: the open order ticket — line items, promo, receipt tally, checkout. */
@Composable
fun CartScreen(
    lines: List<CartLine>,
    totals: CartTotals,
    promoApplied: Boolean,
    onQuantityChange: (CartLine, Int) -> Unit,
    onApplyPromo: (String) -> Boolean,
    onCheckout: () -> Unit,
    onBrowseMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    if (lines.isEmpty()) {
        Column(modifier.fillMaxSize().background(colors.paper)) {
            EmptyState(
                glyph = "+",
                title = stringResource(Res.string.cart_empty_title),
                body = stringResource(Res.string.cart_empty_body),
                actionLabel = stringResource(Res.string.cart_browse_menu),
                onAction = onBrowseMenu,
                modifier = Modifier.padding(top = 72.dp),
            )
        }
        return
    }
    Column(modifier.fillMaxSize().background(colors.paper).padding(20.dp)) {
        ScreenHeader(
            eyebrow = stringResource(Res.string.cart_eyebrow),
            title = stringResource(Res.string.cart_title),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(lines, key = { "${it.item.id}-${it.size}" }) { line ->
                CartLineRow(line = line, onQuantityChange = { onQuantityChange(line, it) })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
            PromoCodeField(applied = promoApplied, onApply = onApplyPromo)
            DashedRule(Modifier.fillMaxWidth())
            TotalsSummary(totals = totals)
            StampButton(
                text = stringResource(Res.string.cart_checkout),
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Cart · filled", group = "Ordering", widthDp = 402, heightDp = 874)
@Composable
fun CartScreenFilledPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Cart, cartCount = 4) { padding ->
            CartScreen(
                lines = SampleData.sampleCart,
                totals = SampleData.sampleTotals,
                promoApplied = false,
                onQuantityChange = { _, _ -> },
                onApplyPromo = { false },
                onCheckout = {},
                onBrowseMenu = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "Cart · empty", group = "Ordering", widthDp = 402, heightDp = 874)
@Composable
fun CartScreenEmptyPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Cart, cartCount = 0) { padding ->
            CartScreen(
                lines = emptyList(),
                totals = CartTotals(0, 0, 0),
                promoApplied = false,
                onQuantityChange = { _, _ -> },
                onApplyPromo = { false },
                onCheckout = {},
                onBrowseMenu = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}
