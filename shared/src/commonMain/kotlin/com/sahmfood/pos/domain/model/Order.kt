package com.sahmfood.pos.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a customer's in-progress or completed order session.
 */
data class Order(
    val id: String,
    val items: List<CartItem>,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val note: String = "",
    val tableNumber: String = "",
    val cashierId: String = "",
) {
    val subtotal: Double get() = items.sumOf { it.lineTotal }

    fun tax(taxRate: Double): Double = subtotal * taxRate

    fun total(
        taxRate: Double = TAX_RATE,
        extraDiscount: Double = 0.0,
    ): Double = subtotal + tax(taxRate) - extraDiscount

    companion object {
        const val TAX_RATE = 0.15 // 15 % VAT — configurable at call site
    }
}

enum class OrderStatus {
    DRAFT, // being assembled in cart
    CONFIRMED, // sent to kitchen / confirmed by cashier
    PAID, // payment complete, receipt issued
    CANCELLED, // voided
    SYNCED, // successfully uploaded to the server
}
