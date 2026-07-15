package com.crowdedlibs.cafe.model

/** Menu sections shown as filter chips on the menu screen. */
enum class MenuCategory {
    Espresso,
    Brew,
    Pastry,
    Sandwich,
}

/** Dietary markers rendered as small tags on cards and the detail screen. */
enum class DietaryTag {
    Vegan,
    Vegetarian,
    GlutenFree,
    DairyFree,
    ContainsNuts,
}

/** Cup/portion size; [priceDeltaCents] is added to the item base price. */
enum class ItemSize(val short: String, val priceDeltaCents: Int) {
    Small("S", 0),
    Medium("M", 50),
    Large("L", 90),
}

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val basePriceCents: Int,
    val category: MenuCategory,
    val tags: Set<DietaryTag> = emptySet(),
    val featured: Boolean = false,
    val soldOut: Boolean = false,
    val sizes: List<ItemSize> = listOf(ItemSize.Small, ItemSize.Medium, ItemSize.Large),
) {
    init {
        require(id.isNotBlank()) { "Menu item id must not be blank" }
        require(name.isNotBlank()) { "Menu item name must not be blank" }
        require(basePriceCents >= 0) { "Menu item price must not be negative" }
        require(sizes.isNotEmpty()) { "Menu item must offer at least one size" }
        require(sizes.distinct().size == sizes.size) { "Menu item sizes must be unique" }
    }
}

data class CartLine(
    val item: MenuItem,
    val size: ItemSize,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Cart quantity must be positive" }
        require(size in item.sizes) { "${item.name} does not offer size ${size.name}" }
    }

    val unitPriceCents: Int get() = item.basePriceCents + size.priceDeltaCents
    val totalCents: Int get() = unitPriceCents * quantity
}

data class CartTotals(
    val subtotalCents: Int,
    val discountCents: Int,
    val taxCents: Int,
) {
    init {
        require(subtotalCents >= 0 && discountCents >= 0 && taxCents >= 0)
        require(discountCents <= subtotalCents)
    }

    val totalCents: Int get() = subtotalCents - discountCents + taxCents
}

enum class OrderStatus {
    Received,
    Preparing,
    Ready,
    PickedUp,
}

data class Order(
    val id: String,
    val lines: List<CartLine>,
    val status: OrderStatus,
    val pickupCode: String,
    val totals: CartTotals,
) {
    init {
        require(lines.isNotEmpty()) { "Order must contain at least one line" }
    }
}

/** Wasm-safe money formatting; the sample keeps all prices in integer cents. */
fun formatPrice(cents: Int): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = kotlin.math.abs(cents.toLong())
    return "$sign\$${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}
