package com.crowdedlibs.cafe.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelsTest {
    @Test
    fun priceFormattingHandlesSignsAndIntMinimum() {
        assertEquals("$0.00", formatPrice(0))
        assertEquals("$12.34", formatPrice(1_234))
        assertEquals("-$0.01", formatPrice(-1))
        assertEquals("-$21474836.48", formatPrice(Int.MIN_VALUE))
    }

    @Test
    fun modelInvariantsRejectInvalidCartData() {
        val item = MenuItem("coffee", "Coffee", "", 300, MenuCategory.Brew)
        assertFailsWith<IllegalArgumentException> { CartLine(item, ItemSize.Small, 0) }
        assertFailsWith<IllegalArgumentException> { CartTotals(100, 101, 0) }
    }
}
