package com.crowdedlibs.cafe.state

import com.crowdedlibs.cafe.data.SampleData
import com.crowdedlibs.cafe.model.ItemSize
import com.crowdedlibs.cafe.model.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CafeStateTest {
    @Test
    fun cartMergesLinesAndCalculatesDiscountedTotals() {
        val state = CafeState()
        val item = SampleData.menu.first { !it.soldOut && ItemSize.Medium in it.sizes }
        state.addToCart(item, ItemSize.Medium, 1)
        state.addToCart(item, ItemSize.Medium, 2)

        assertEquals(3, state.cart.single().quantity)
        assertTrue(state.applyPromo(" crema "))
        val subtotal = (item.basePriceCents + ItemSize.Medium.priceDeltaCents) * 3
        assertEquals(subtotal, state.totals.subtotalCents)
        assertEquals(subtotal / 10, state.totals.discountCents)
    }

    @Test
    fun invalidCartOperationsAreRejected() {
        val state = CafeState()
        val soldOut = SampleData.menu.first { it.soldOut }
        assertFailsWith<IllegalArgumentException> {
            state.addToCart(soldOut, soldOut.sizes.first(), 1)
        }
        assertFailsWith<IllegalStateException> { state.placeOrder() }
        assertFalse(state.applyPromo("wrong"))
    }

    @Test
    fun placingAnOrderUsesOneNumberAndClearsTransientCartState() {
        val state = CafeState()
        val item = SampleData.menu.first { !it.soldOut }
        state.addToCart(item, item.sizes.first(), 1)
        state.applyPromo("CREMA")

        val order = state.placeOrder()

        assertEquals("order-1042", order.id)
        assertEquals("C-42", order.pickupCode)
        assertTrue(state.cart.isEmpty())
        assertFalse(state.promoApplied)
        state.advanceOrder()
        assertEquals(OrderStatus.Preparing, state.activeOrder?.status)
    }
}
