package com.crowdedlibs.cafe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.model.DietaryTag
import com.crowdedlibs.cafe.model.MenuCategory
import com.crowdedlibs.cafe.ui.label
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * The section headings, set as small stamped tickets. The open section is
 * struck in stamp orange; the rest are hairline outlines. `null` means "All".
 */
@Composable
fun CategoryChipRow(
    selected: MenuCategory?,
    onSelected: (MenuCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TicketChip(label = null.label(), selected = selected == null, onClick = { onSelected(null) })
        MenuCategory.entries.forEach { category ->
            TicketChip(
                label = category.label(),
                selected = selected == category,
                onClick = { onSelected(category) },
            )
        }
    }
}

@Composable
private fun TicketChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    val shape = RoundedCornerShape(2.dp)
    val fill by animateColorAsState(if (selected) colors.stamp else Color.Transparent, label = "chip-fill")
    val textColor = if (selected) colors.ticket else colors.ink
    Text(
        text = label,
        style = type.label.copy(color = textColor),
        modifier = Modifier
            .clip(shape)
            .background(fill, shape)
            .border(1.dp, if (selected) colors.stamp else colors.line, shape)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Dietary markers as small hairline tickets with a leading mono bullet. */
@Composable
fun DietaryTagRow(
    tags: Set<DietaryTag>,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.sorted().forEach { tag ->
            Text(
                text = "• " + tag.label(),
                style = type.mono.copy(color = colors.inkSoft),
                modifier = Modifier
                    .hairline()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Preview(name = "Category filter", group = "Chips", widthDp = 420)
@Composable
fun CategoryChipRowPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CategoryChipRow(selected = MenuCategory.Espresso, onSelected = {}, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "Dietary tags", group = "Chips", widthDp = 420)
@Composable
fun DietaryTagsPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        DietaryTagRow(
            tags = setOf(DietaryTag.Vegan, DietaryTag.GlutenFree, DietaryTag.ContainsNuts),
            modifier = Modifier.padding(12.dp),
        )
    }
}
