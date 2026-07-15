package com.crowdedlibs.cafe.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.MenuItem
import com.crowdedlibs.cafe.model.formatPrice
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.menu_featured
import com.crowdedlibs.cafe.resources.menu_sold_out
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A line on the menu, set like a printed carte: the item name, a run of dot
 * leaders, then the price in the receipt monospace. Sold-out lines fade and
 * take a rotated "sold out" ink stamp across the price.
 */
@Composable
fun MenuItemCard(
    item: MenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    PaperCard(
        modifier = modifier.fillMaxWidth().let {
            if (item.soldOut) it else it.clickable(onClick = onClick)
        },
    ) {
        Row(
            Modifier.padding(14.dp).alpha(if (item.soldOut) 0.55f else 1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Monogram(item.name, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(item.name, style = type.serifItem.copy(color = colors.ink))
                    DashedRule(
                        Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 7.dp),
                    )
                    Text(formatPrice(item.basePriceCents), style = type.price.copy(color = colors.ink))
                }
                Text(
                    item.description,
                    style = type.bodySmall.copy(color = colors.inkSoft),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.soldOut) {
            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.CenterEnd) {
                StampMark(stringResource(Res.string.menu_sold_out), rotate = -8f)
            }
        }
    }
}

/**
 * A hero "special" pulled from the board: a dark chalkboard panel in the
 * featured strip, chalk lettering, stamp-orange price — the specials the café
 * chalks up top, in contrast to the cream menu rows below.
 */
@Composable
fun FeaturedItemCard(
    item: MenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    PaperCard(
        modifier = modifier.width(248.dp).clickable(onClick = onClick),
        color = colors.board,
        border = false,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Monogram(item.name, size = 34.dp)
                Spacer(Modifier.weight(1f))
                StampMark(stringResource(Res.string.menu_featured), color = colors.stamp, rotate = 3f)
            }
            Text(
                item.name,
                style = type.heading.copy(color = colors.chalk),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.description,
                style = type.bodySmall.copy(color = colors.chalk.copy(alpha = 0.7f)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatPrice(item.basePriceCents), style = type.price.copy(color = colors.stamp))
        }
    }
}

@Preview(name = "Menu item · default", group = "Cards", widthDp = 360)
@Composable
fun MenuItemCardPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        MenuItemCard(item = SampleData.item("flat-white"), onClick = {}, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "Menu item · sold out", group = "Cards", widthDp = 360)
@Composable
fun MenuItemCardSoldOutPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        MenuItemCard(item = SampleData.item("mocha"), onClick = {}, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "Featured special", group = "Cards", widthDp = 300)
@Composable
fun FeaturedItemCardPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        FeaturedItemCard(item = SampleData.item("pour-over"), onClick = {}, modifier = Modifier.padding(8.dp))
    }
}
