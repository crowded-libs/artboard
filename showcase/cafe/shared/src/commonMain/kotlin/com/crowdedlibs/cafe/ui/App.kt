package com.crowdedlibs.cafe.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.MenuCategory
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.tab_cart
import com.crowdedlibs.cafe.resources.tab_menu
import com.crowdedlibs.cafe.resources.tab_orders
import com.crowdedlibs.cafe.resources.tab_settings
import com.crowdedlibs.cafe.state.CafeState
import com.crowdedlibs.cafe.ui.screens.CartScreen
import com.crowdedlibs.cafe.ui.screens.CheckoutScreen
import com.crowdedlibs.cafe.ui.screens.ItemDetailScreen
import com.crowdedlibs.cafe.ui.screens.MenuScreen
import com.crowdedlibs.cafe.ui.screens.OrderStatusScreen
import com.crowdedlibs.cafe.ui.components.Text
import com.crowdedlibs.cafe.ui.screens.SettingsScreen
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** App destinations; a plain sealed hierarchy keeps navigation wasm-safe. */
sealed interface CafeDestination {
    data object Menu : CafeDestination
    data class ItemDetail(val itemId: String) : CafeDestination
    data object Cart : CafeDestination
    data object Checkout : CafeDestination
    data object Orders : CafeDestination
    data object Settings : CafeDestination
}

/** Counter-bar tabs; pushed destinations highlight their owning tab. */
enum class CafeTab(val label: StringResource) {
    Menu(Res.string.tab_menu),
    Cart(Res.string.tab_cart),
    Orders(Res.string.tab_orders),
    Settings(Res.string.tab_settings),
}

/**
 * The full app chrome. Instead of a Material `NavigationBar`, the bottom is a
 * flat chalkboard "counter bar": the four sections written out in chalk, the
 * open one struck with a stamp-orange dot. Screen previews wrap themselves in
 * this frame so a "Screen" frame on the Artboard board shows exactly what a
 * device shows — chrome included.
 */
@Composable
fun CafeAppFrame(
    selectedTab: CafeTab,
    cartCount: Int,
    modifier: Modifier = Modifier,
    onTabSelected: (CafeTab) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = CafeTheme.colors
    Column(modifier.fillMaxSize().background(colors.paper)) {
        Box(Modifier.weight(1f)) { content(PaddingValues(0.dp)) }
        CounterBar(selectedTab = selectedTab, cartCount = cartCount, onTabSelected = onTabSelected)
    }
}

@Composable
private fun CounterBar(
    selectedTab: CafeTab,
    cartCount: Int,
    onTabSelected: (CafeTab) -> Unit,
) {
    val colors = CafeTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.board)) {
        // A chalk hairline where the board meets the wall.
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.chalk.copy(alpha = 0.14f)))
        Row(
            Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CafeTab.entries.forEach { tab ->
                CounterTab(
                    label = stringResource(tab.label),
                    selected = tab == selectedTab,
                    badge = if (tab == CafeTab.Cart && cartCount > 0) cartCount else null,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CounterTab(
    label: String,
    selected: Boolean,
    badge: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    val dotColor by animateColorAsState(
        if (selected) colors.stamp else Color.Transparent,
        label = "tab-dot",
    )
    val dotSize by animateDpAsState(if (selected) 6.dp else 0.dp, label = "tab-dot-size")
    val labelColor = if (selected) colors.chalk else colors.chalk.copy(alpha = 0.55f)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = type.label.copy(color = labelColor), textAlign = TextAlign.Center)
            if (badge != null) {
                Spacer(Modifier.width(4.dp))
                Text("·$badge", style = type.mono.copy(color = colors.stamp))
            }
        }
    }
}

/**
 * Product entrypoint used by Android and iOS. Owns theme, navigation, and the
 * [CafeState]; individual screens read the design system from [CafeTheme] and
 * each preview re-applies it, so the Artboard gallery shows the real café.
 */
@Composable
fun CafeApp(
    darkTheme: Boolean = isSystemInDarkTheme(),
    applySafeAreaInsets: Boolean = true,
) {
    val state = remember { CafeState() }
    var destination by remember { mutableStateOf<CafeDestination>(CafeDestination.Menu) }
    var selectedCategory by remember { mutableStateOf<MenuCategory?>(null) }
    var orderUpdates by remember { mutableStateOf(true) }
    var promotions by remember { mutableStateOf(false) }

    val selectedTab = when (destination) {
        CafeDestination.Menu, is CafeDestination.ItemDetail -> CafeTab.Menu
        CafeDestination.Cart, CafeDestination.Checkout -> CafeTab.Cart
        CafeDestination.Orders -> CafeTab.Orders
        CafeDestination.Settings -> CafeTab.Settings
    }

    CafeTheme(darkTheme = darkTheme) {
        CafeAppFrame(
            modifier = if (applySafeAreaInsets) {
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            } else {
                Modifier
            },
            selectedTab = selectedTab,
            cartCount = state.cart.sumOf { it.quantity },
            onTabSelected = { tab ->
                destination = when (tab) {
                    CafeTab.Menu -> CafeDestination.Menu
                    CafeTab.Cart -> CafeDestination.Cart
                    CafeTab.Orders -> CafeDestination.Orders
                    CafeTab.Settings -> CafeDestination.Settings
                }
            },
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (val dest = destination) {
                CafeDestination.Menu -> MenuScreen(
                    state = state.menuState,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    onItemClick = { destination = CafeDestination.ItemDetail(it.id) },
                    modifier = contentModifier,
                )

                is CafeDestination.ItemDetail -> ItemDetailScreen(
                    item = SampleData.item(dest.itemId),
                    onAddToCart = { size, quantity ->
                        state.addToCart(SampleData.item(dest.itemId), size, quantity)
                        destination = CafeDestination.Cart
                    },
                    onBack = { destination = CafeDestination.Menu },
                    modifier = contentModifier,
                )

                CafeDestination.Cart -> CartScreen(
                    lines = state.cart,
                    totals = state.totals,
                    promoApplied = state.promoApplied,
                    onQuantityChange = state::setQuantity,
                    onApplyPromo = { state.applyPromo(it) },
                    onCheckout = { destination = CafeDestination.Checkout },
                    onBrowseMenu = { destination = CafeDestination.Menu },
                    modifier = contentModifier,
                )

                CafeDestination.Checkout -> CheckoutScreen(
                    lines = state.cart,
                    totals = state.totals,
                    onPlaceOrder = {
                        state.placeOrder()
                        destination = CafeDestination.Orders
                    },
                    onBack = { destination = CafeDestination.Cart },
                    modifier = contentModifier,
                )

                CafeDestination.Orders -> OrderStatusScreen(
                    order = state.activeOrder,
                    hasError = false,
                    onRetry = {},
                    onAdvanceDemo = state::advanceOrder,
                    modifier = contentModifier,
                )

                CafeDestination.Settings -> SettingsScreen(
                    orderUpdates = orderUpdates,
                    promotions = promotions,
                    onOrderUpdatesChange = { orderUpdates = it },
                    onPromotionsChange = { promotions = it },
                    modifier = contentModifier,
                )
            }
        }
    }
}
