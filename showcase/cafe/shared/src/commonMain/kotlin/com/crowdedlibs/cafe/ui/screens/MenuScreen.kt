package com.crowdedlibs.cafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.MenuCategory
import com.crowdedlibs.cafe.model.MenuItem
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.menu_empty_body
import com.crowdedlibs.cafe.resources.menu_empty_title
import com.crowdedlibs.cafe.resources.menu_eyebrow
import com.crowdedlibs.cafe.resources.menu_loading
import com.crowdedlibs.cafe.resources.menu_specials
import com.crowdedlibs.cafe.resources.menu_subtitle
import com.crowdedlibs.cafe.resources.menu_title
import com.crowdedlibs.cafe.state.MenuUiState
import com.crowdedlibs.cafe.ui.CafeAppFrame
import com.crowdedlibs.cafe.ui.CafeTab
import com.crowdedlibs.cafe.ui.components.BoardHero
import com.crowdedlibs.cafe.ui.components.CategoryChipRow
import com.crowdedlibs.cafe.ui.components.EmptyState
import com.crowdedlibs.cafe.ui.components.FeaturedItemCard
import com.crowdedlibs.cafe.ui.components.LoadingListPlaceholder
import com.crowdedlibs.cafe.ui.components.MenuItemCard
import com.crowdedlibs.cafe.ui.components.SettleIn
import com.crowdedlibs.cafe.ui.components.Text
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** Home tab: chalkboard hero, specials strip, section filter, and the carte. */
@Composable
fun MenuScreen(
    state: MenuUiState,
    selectedCategory: MenuCategory?,
    onCategorySelected: (MenuCategory?) -> Unit,
    onItemClick: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(modifier.fillMaxSize().background(colors.paper)) {
        BoardHero(
            eyebrow = stringResource(Res.string.menu_eyebrow),
            title = stringResource(Res.string.menu_title),
            subtitle = stringResource(Res.string.menu_subtitle),
        )

        when (state) {
            MenuUiState.Loading -> Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(Res.string.menu_loading), style = type.mono.copy(color = colors.inkSoft))
                LoadingListPlaceholder(rows = 5)
            }

            MenuUiState.Empty -> EmptyState(
                glyph = "·",
                title = stringResource(Res.string.menu_empty_title),
                body = stringResource(Res.string.menu_empty_body),
                modifier = Modifier.padding(top = 48.dp),
            )

            is MenuUiState.Loaded -> {
                val featured = state.items.filter { it.featured && !it.soldOut }
                val visible = state.items.filter {
                    selectedCategory == null || it.category == selectedCategory
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (featured.isNotEmpty() && selectedCategory == null) {
                        item {
                            Text(
                                stringResource(Res.string.menu_specials).uppercase(),
                                style = type.label.copy(color = colors.inkSoft),
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(featured, key = { "featured-${it.id}" }) { item ->
                                    FeaturedItemCard(item = item, onClick = { onItemClick(item) })
                                }
                            }
                        }
                    }
                    item {
                        CategoryChipRow(
                            selected = selectedCategory,
                            onSelected = onCategorySelected,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                    if (visible.isEmpty()) {
                        item {
                            EmptyState(
                                glyph = "·",
                                title = stringResource(Res.string.menu_empty_title),
                                body = stringResource(Res.string.menu_empty_body),
                                modifier = Modifier.padding(top = 28.dp),
                            )
                        }
                    }
                    itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                        SettleIn(index = index) {
                            MenuItemCard(item = item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }
    }
}

// Screen previews apply the product theme and frame so the board shows the same
// composition as an Android or iOS device.

@Preview(name = "Menu · populated", group = "Menu", widthDp = 402, heightDp = 874)
@Composable
fun MenuScreenPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Menu, cartCount = 2) { padding ->
            MenuScreen(
                state = MenuUiState.Loaded(SampleData.menu),
                selectedCategory = null,
                onCategorySelected = {},
                onItemClick = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "Menu · night", group = "Menu", widthDp = 402, heightDp = 874)
@Composable
fun MenuScreenNightPreview() {
    CafeTheme(darkTheme = true) {
        CafeAppFrame(selectedTab = CafeTab.Menu, cartCount = 2) { padding ->
            MenuScreen(
                state = MenuUiState.Loaded(SampleData.menu),
                selectedCategory = null,
                onCategorySelected = {},
                onItemClick = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "Menu · loading", group = "Menu", widthDp = 402, heightDp = 874)
@Composable
fun MenuScreenLoadingPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Menu, cartCount = 0) { padding ->
            MenuScreen(
                state = MenuUiState.Loading,
                selectedCategory = null,
                onCategorySelected = {},
                onItemClick = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(name = "Menu · empty", group = "Menu", widthDp = 402, heightDp = 874)
@Composable
fun MenuScreenEmptyPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Menu, cartCount = 0) { padding ->
            MenuScreen(
                state = MenuUiState.Empty,
                selectedCategory = null,
                onCategorySelected = {},
                onItemClick = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}
