package com.crowdedlibs.cafe.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.CartLine
import com.crowdedlibs.cafe.model.CartTotals
import com.crowdedlibs.cafe.model.ItemSize
import com.crowdedlibs.cafe.model.MenuItem
import com.crowdedlibs.cafe.model.Order
import com.crowdedlibs.cafe.model.OrderStatus

/** Menu screen content phases; previews cover each variant explicitly. */
sealed interface MenuUiState {
    data object Loading : MenuUiState
    data object Empty : MenuUiState
    data class Loaded(val items: List<MenuItem>) : MenuUiState
}

private const val TAX_PERMILLE = 90 // 9% flat demo tax
private const val PROMO_CODE = "CREMA"
private const val PROMO_PERMILLE = 100 // 10% off subtotal

/**
 * Single in-memory state holder for the whole app. Deliberately plain —
 * no DI, persistence, or platform APIs — so it runs identically on
 * Android, iOS, and the wasm preview host.
 */
class CafeState {

    var menuState: MenuUiState by mutableStateOf(MenuUiState.Loaded(SampleData.menu))
        private set

    val cart = mutableStateListOf<CartLine>()

    var promoApplied: Boolean by mutableStateOf(false)
        private set

    var activeOrder: Order? by mutableStateOf(null)
        private set

    private var orderCounter = 1042

    val totals: CartTotals
        get() {
            val subtotal = cart.sumOf { it.totalCents }
            val discount = if (promoApplied) subtotal * PROMO_PERMILLE / 1000 else 0
            val tax = (subtotal - discount) * TAX_PERMILLE / 1000
            return CartTotals(subtotal, discount, tax)
        }

    fun addToCart(item: MenuItem, size: ItemSize, quantity: Int) {
        require(!item.soldOut) { "Sold-out items cannot be added to the cart" }
        require(size in item.sizes) { "${item.name} does not offer size ${size.name}" }
        require(quantity > 0) { "Quantity must be positive" }
        val index = cart.indexOfFirst { it.item.id == item.id && it.size == size }
        if (index >= 0) {
            val existing = cart[index]
            cart[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            cart += CartLine(item, size, quantity)
        }
    }

    fun setQuantity(line: CartLine, quantity: Int) {
        val index = cart.indexOf(line)
        if (index < 0) return
        if (quantity <= 0) cart.removeAt(index) else cart[index] = line.copy(quantity = quantity)
    }

    fun removeLine(line: CartLine) {
        cart.remove(line)
    }

    /** Returns true when [code] is the demo promo code. */
    fun applyPromo(code: String): Boolean {
        promoApplied = code.trim().uppercase() == PROMO_CODE
        return promoApplied
    }

    fun placeOrder(): Order {
        check(cart.isNotEmpty()) { "Cannot place an empty order" }
        val orderNumber = orderCounter++
        val order = Order(
            id = "order-$orderNumber",
            lines = cart.toList(),
            status = OrderStatus.Received,
            pickupCode = "C-${orderNumber % 100}",
            totals = totals,
        )
        activeOrder = order
        cart.clear()
        promoApplied = false
        return order
    }

    /** Demo helper: nudge the active order through its lifecycle. */
    fun advanceOrder() {
        val order = activeOrder ?: return
        val next = OrderStatus.entries.getOrNull(order.status.ordinal + 1) ?: return
        activeOrder = order.copy(status = next)
    }
}
