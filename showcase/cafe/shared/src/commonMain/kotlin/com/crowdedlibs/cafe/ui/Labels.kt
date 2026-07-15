package com.crowdedlibs.cafe.ui

import androidx.compose.runtime.Composable
import com.crowdedlibs.cafe.model.DietaryTag
import com.crowdedlibs.cafe.model.MenuCategory
import com.crowdedlibs.cafe.model.OrderStatus
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.category_all
import com.crowdedlibs.cafe.resources.category_brew
import com.crowdedlibs.cafe.resources.category_espresso
import com.crowdedlibs.cafe.resources.category_pastry
import com.crowdedlibs.cafe.resources.category_sandwich
import com.crowdedlibs.cafe.resources.order_status_picked_up
import com.crowdedlibs.cafe.resources.order_status_preparing
import com.crowdedlibs.cafe.resources.order_status_ready
import com.crowdedlibs.cafe.resources.order_status_received
import com.crowdedlibs.cafe.resources.tag_dairy_free
import com.crowdedlibs.cafe.resources.tag_gluten_free
import com.crowdedlibs.cafe.resources.tag_nuts
import com.crowdedlibs.cafe.resources.tag_vegan
import com.crowdedlibs.cafe.resources.tag_vegetarian
import org.jetbrains.compose.resources.stringResource

/** Localized display names for data-layer enums. */

@Composable
fun MenuCategory?.label(): String = when (this) {
    null -> stringResource(Res.string.category_all)
    MenuCategory.Espresso -> stringResource(Res.string.category_espresso)
    MenuCategory.Brew -> stringResource(Res.string.category_brew)
    MenuCategory.Pastry -> stringResource(Res.string.category_pastry)
    MenuCategory.Sandwich -> stringResource(Res.string.category_sandwich)
}

@Composable
fun DietaryTag.label(): String = when (this) {
    DietaryTag.Vegan -> stringResource(Res.string.tag_vegan)
    DietaryTag.Vegetarian -> stringResource(Res.string.tag_vegetarian)
    DietaryTag.GlutenFree -> stringResource(Res.string.tag_gluten_free)
    DietaryTag.DairyFree -> stringResource(Res.string.tag_dairy_free)
    DietaryTag.ContainsNuts -> stringResource(Res.string.tag_nuts)
}

@Composable
fun OrderStatus.label(): String = when (this) {
    OrderStatus.Received -> stringResource(Res.string.order_status_received)
    OrderStatus.Preparing -> stringResource(Res.string.order_status_preparing)
    OrderStatus.Ready -> stringResource(Res.string.order_status_ready)
    OrderStatus.PickedUp -> stringResource(Res.string.order_status_picked_up)
}
