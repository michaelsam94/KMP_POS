package com.sahmfood.pos.domain.model

/**
 * A product line inside an active cart/order.
 * Quantity is always ≥ 1; use cart operations to remove.
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
    val unitPrice: Double = product.price,
    val discount: Double = 0.0, // flat amount per unit
) {
    init {
        require(quantity >= 1) { "CartItem quantity must be at least 1" }
        require(unitPrice >= 0.0) { "Unit price must be non-negative" }
        require(discount >= 0.0 && discount <= unitPrice) {
            "Discount ($discount) must be between 0 and unitPrice ($unitPrice)"
        }
    }

    val lineTotal: Double get() = (unitPrice - discount) * quantity
}
