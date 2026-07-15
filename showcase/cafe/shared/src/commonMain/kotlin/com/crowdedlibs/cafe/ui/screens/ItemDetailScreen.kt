package com.crowdedlibs.cafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.ItemSize
import com.crowdedlibs.cafe.model.MenuItem
import com.crowdedlibs.cafe.model.formatPrice
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.detail_quantity
import com.crowdedlibs.cafe.resources.detail_size
import com.crowdedlibs.cafe.ui.CafeAppFrame
import com.crowdedlibs.cafe.ui.CafeTab
import com.crowdedlibs.cafe.ui.components.AddToCartBar
import com.crowdedlibs.cafe.ui.components.BackLink
import com.crowdedlibs.cafe.ui.components.DietaryTagRow
import com.crowdedlibs.cafe.ui.components.Monogram
import com.crowdedlibs.cafe.ui.components.QuantityStepper
import com.crowdedlibs.cafe.ui.components.SizeSelector
import com.crowdedlibs.cafe.ui.components.Text
import com.crowdedlibs.cafe.ui.label
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** Item customization: cup mark, price, size, quantity, dietary info, add. */
@Composable
fun ItemDetailScreen(
    item: MenuItem,
    onAddToCart: (ItemSize, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    var size by remember { mutableStateOf(item.sizes.getOrElse(1) { item.sizes.first() }) }
    var quantity by remember { mutableIntStateOf(1) }

    Column(modifier.fillMaxSize().background(colors.paper).padding(20.dp)) {
        BackLink(text = item.category.label(), onClick = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Monogram(item.name, size = 96.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, style = type.board.copy(color = colors.ink))
                Text(formatPrice(item.basePriceCents), style = type.price.copy(color = colors.stamp))
            }
            Text(item.description, style = type.body.copy(color = colors.ink))
            if (item.tags.isNotEmpty()) {
                DietaryTagRow(tags = item.tags)
            }
            if (item.sizes.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.detail_size).uppercase(), style = type.label.copy(color = colors.inkSoft))
                    SizeSelector(
                        sizes = item.sizes,
                        selected = size,
                        onSelected = { size = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.detail_quantity).uppercase(), style = type.label.copy(color = colors.inkSoft))
                Spacer(Modifier.weight(1f))
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = { if (it in 1..9) quantity = it },
                )
            }
        }
        AddToCartBar(
            totalCents = (item.basePriceCents + size.priceDeltaCents) * quantity,
            onAddToCart = { onAddToCart(size, quantity) },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Preview(name = "Item detail", group = "Menu", widthDp = 402, heightDp = 874)
@Composable
fun ItemDetailScreenPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Menu, cartCount = 2) { padding ->
            ItemDetailScreen(
                item = SampleData.item("oat-latte"),
                onAddToCart = { _, _ -> },
                onBack = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}
