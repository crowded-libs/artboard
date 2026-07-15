package com.crowdedlibs.cafe.data

import com.crowdedlibs.cafe.model.CartLine
import com.crowdedlibs.cafe.model.CartTotals
import com.crowdedlibs.cafe.model.DietaryTag
import com.crowdedlibs.cafe.model.ItemSize
import com.crowdedlibs.cafe.model.MenuCategory
import com.crowdedlibs.cafe.model.MenuItem
import com.crowdedlibs.cafe.model.Order
import com.crowdedlibs.cafe.model.OrderStatus

/**
 * In-memory catalog shared by the running app and every `@Preview`.
 *
 * Item names stay unlocalized on purpose — they are data, not chrome; the
 * locale demo lives in the surrounding labels via compose resources.
 */
object SampleData {

    val menu: List<MenuItem> = listOf(
        MenuItem(
            id = "espresso-solo",
            name = "Espresso",
            description = "Double shot, chocolate-forward house blend.",
            basePriceCents = 300,
            category = MenuCategory.Espresso,
            tags = setOf(DietaryTag.Vegan, DietaryTag.GlutenFree),
            sizes = listOf(ItemSize.Small, ItemSize.Medium),
        ),
        MenuItem(
            id = "flat-white",
            name = "Flat White",
            description = "Silky microfoam over a ristretto double.",
            basePriceCents = 450,
            category = MenuCategory.Espresso,
            tags = setOf(DietaryTag.Vegetarian, DietaryTag.GlutenFree),
            featured = true,
        ),
        MenuItem(
            id = "oat-latte",
            name = "Oat Latte",
            description = "House espresso with barista oat milk.",
            basePriceCents = 500,
            category = MenuCategory.Espresso,
            tags = setOf(DietaryTag.Vegan, DietaryTag.DairyFree),
        ),
        MenuItem(
            id = "mocha",
            name = "Mocha",
            description = "Espresso, single-origin cocoa, steamed milk.",
            basePriceCents = 525,
            category = MenuCategory.Espresso,
            tags = setOf(DietaryTag.Vegetarian),
            soldOut = true,
        ),
        MenuItem(
            id = "pour-over",
            name = "Pour Over",
            description = "Single-origin, brewed to order. Ask for today's card.",
            basePriceCents = 475,
            category = MenuCategory.Brew,
            tags = setOf(DietaryTag.Vegan, DietaryTag.GlutenFree),
            featured = true,
        ),
        MenuItem(
            id = "cold-brew",
            name = "Cold Brew",
            description = "18-hour steep, served over clear ice.",
            basePriceCents = 450,
            category = MenuCategory.Brew,
            tags = setOf(DietaryTag.Vegan, DietaryTag.GlutenFree),
        ),
        MenuItem(
            id = "matcha",
            name = "Matcha Whisk",
            description = "Ceremonial grade, whisked with your milk of choice.",
            basePriceCents = 550,
            category = MenuCategory.Brew,
            tags = setOf(DietaryTag.Vegetarian, DietaryTag.GlutenFree),
        ),
        MenuItem(
            id = "croissant",
            name = "Butter Croissant",
            description = "Laminated over three days. Flaky, unapologetic.",
            basePriceCents = 375,
            category = MenuCategory.Pastry,
            tags = setOf(DietaryTag.Vegetarian),
            featured = true,
            sizes = listOf(ItemSize.Medium),
        ),
        MenuItem(
            id = "almond-croissant",
            name = "Almond Croissant",
            description = "Twice-baked with frangipane and toasted almonds.",
            basePriceCents = 450,
            category = MenuCategory.Pastry,
            tags = setOf(DietaryTag.Vegetarian, DietaryTag.ContainsNuts),
            sizes = listOf(ItemSize.Medium),
        ),
        MenuItem(
            id = "banana-bread",
            name = "Banana Bread",
            description = "Thick-cut, toasted, salted butter on the side.",
            basePriceCents = 400,
            category = MenuCategory.Pastry,
            tags = setOf(DietaryTag.Vegetarian, DietaryTag.ContainsNuts),
            sizes = listOf(ItemSize.Medium),
            soldOut = true,
        ),
        MenuItem(
            id = "caprese",
            name = "Caprese Ciabatta",
            description = "Mozzarella, heirloom tomato, basil, olive oil.",
            basePriceCents = 850,
            category = MenuCategory.Sandwich,
            tags = setOf(DietaryTag.Vegetarian),
            sizes = listOf(ItemSize.Medium),
        ),
        MenuItem(
            id = "smoked-turkey",
            name = "Smoked Turkey Melt",
            description = "House-smoked turkey, gruyère, pickled onion.",
            basePriceCents = 950,
            category = MenuCategory.Sandwich,
            sizes = listOf(ItemSize.Medium),
        ),
    )

    fun item(id: String): MenuItem = menu.first { it.id == id }

    /** A representative filled cart for previews and the demo order. */
    val sampleCart: List<CartLine> = listOf(
        CartLine(item("flat-white"), ItemSize.Medium, quantity = 2),
        CartLine(item("croissant"), ItemSize.Medium, quantity = 1),
        CartLine(item("cold-brew"), ItemSize.Large, quantity = 1),
    )

    val sampleTotals: CartTotals = CartTotals(
        subtotalCents = sampleCart.sumOf { it.totalCents },
        discountCents = 0,
        taxCents = sampleCart.sumOf { it.totalCents } * 9 / 100,
    )

    val sampleOrder: Order = Order(
        id = "order-1042",
        lines = sampleCart,
        status = OrderStatus.Preparing,
        pickupCode = "C-42",
        totals = sampleTotals,
    )
}
