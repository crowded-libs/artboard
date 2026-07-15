package com.crowdedlibs.cafe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.ItemSize
import com.crowdedlibs.cafe.model.formatPrice
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.detail_add_to_cart
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** A café toggle: a ticket-shaped track and a paper thumb that slides to the stamp. */
@Composable
fun CafeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val track by animateColorAsState(if (checked) colors.stamp else colors.paper, label = "switch-track")
    val borderColor by animateColorAsState(if (checked) colors.stamp else colors.line, label = "switch-border")
    val thumbX by androidx.compose.animation.core.animateDpAsState(if (checked) 22.dp else 2.dp, label = "switch-thumb")
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = thumbX)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) colors.ticket else colors.inkSoft),
        )
    }
}

/** − / count / + control, set as three ticket cells with a mono count. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(
        modifier = modifier.hairline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperKey("−", "Remove one") { onQuantityChange(quantity - 1) }
        Text(
            quantity.toString(),
            style = type.price.copy(color = colors.ink),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 36.dp),
        )
        StepperKey("+", "Add one") { onQuantityChange(quantity + 1) }
    }
}

@Composable
private fun StepperKey(glyph: String, description: String, onClick: () -> Unit) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Box(
        Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = type.price.copy(color = colors.stamp, fontSize = 20.sp))
    }
}

/** S / M / L picker as ticket cells; the chosen size fills with chalkboard. */
@Composable
fun SizeSelector(
    sizes: List<ItemSize>,
    selected: ItemSize,
    onSelected: (ItemSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(modifier = modifier.hairline().clip(RoundedCornerShape(3.dp))) {
        sizes.forEach { size ->
            val chosen = size == selected
            val fill by animateColorAsState(if (chosen) colors.board else Color.Transparent, label = "size-fill")
            val delta = if (size.priceDeltaCents > 0) " +" + formatPrice(size.priceDeltaCents) else ""
            Text(
                text = size.short + delta,
                style = type.mono.copy(color = if (chosen) colors.chalk else colors.ink),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(fill)
                    .selectable(
                        selected = chosen,
                        role = Role.RadioButton,
                        onClick = { onSelected(size) },
                    )
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/**
 * The order action, given the one saturated stamp fill on the screen — a paid
 * receipt about to print. Total in the receipt monospace on the right.
 */
@Composable
fun AddToCartBar(
    totalCents: Int,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colors.stamp)
            .clickable(onClick = onAddToCart)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(Res.string.detail_add_to_cart), style = type.label.copy(color = colors.ticket))
        Text(formatPrice(totalCents), style = type.price.copy(color = colors.ticket))
    }
}

@Preview(name = "Quantity stepper", group = "Controls")
@Composable
fun QuantityStepperPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) { QuantityStepper(quantity = 2, onQuantityChange = {}, modifier = Modifier.padding(12.dp)) }
}

@Preview(name = "Size selector", group = "Controls", widthDp = 360)
@Composable
fun SizeSelectorPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        SizeSelector(
            sizes = SampleData.item("flat-white").sizes,
            selected = ItemSize.Medium,
            onSelected = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(name = "Add to cart bar", group = "Controls", widthDp = 360)
@Composable
fun AddToCartBarPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) { AddToCartBar(totalCents = 950, onAddToCart = {}, modifier = Modifier.padding(12.dp)) }
}
