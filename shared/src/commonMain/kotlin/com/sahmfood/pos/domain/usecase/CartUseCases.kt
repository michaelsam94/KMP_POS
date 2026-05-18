package com.sahmfood.pos.domain.usecase

import com.sahmfood.pos.domain.model.CartItem
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.repository.OrderRepository
import com.benasher44.uuid.uuid4
import kotlinx.datetime.Clock

/**
 * Adds a product to the cart of the given order.
 * If the product is already in the cart, its quantity is incremented.
 */
class AddItemToCartUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(order: Order, product: Product, quantity: Int = 1): Result<Order> {
        return runCatching {
            require(quantity >= 1) { "Quantity must be at least 1" }
            val existing = order.items.find { it.product.id == product.id }
            val updatedItems = if (existing != null) {
                order.items.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                order.items + CartItem(product = product, quantity = quantity)
            }
            val updated = order.copy(items = updatedItems, updatedAt = Clock.System.now())
            orderRepository.save(updated)
            updated
        }
    }
}

/** Removes a specific product line from the cart entirely. */
class RemoveItemFromCartUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(order: Order, productId: String): Result<Order> {
        return runCatching {
            val updated = order.copy(
                items = order.items.filter { it.product.id != productId },
                updatedAt = Clock.System.now()
            )
            orderRepository.save(updated)
            updated
        }
    }
}

/** Sets the exact quantity for a cart line; removes the line when quantity reaches 0. */
class UpdateCartItemQuantityUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(order: Order, productId: String, newQuantity: Int): Result<Order> {
        return runCatching {
            require(newQuantity >= 0) { "Quantity cannot be negative" }
            val updatedItems = if (newQuantity == 0) {
                order.items.filter { it.product.id != productId }
            } else {
                order.items.map {
                    if (it.product.id == productId) it.copy(quantity = newQuantity) else it
                }
            }
            val updated = order.copy(items = updatedItems, updatedAt = Clock.System.now())
            orderRepository.save(updated)
            updated
        }
    }
}

/** Creates a fresh empty order in DRAFT status and persists it. */
class CreateOrderUseCase(private val orderRepository: OrderRepository) {
    suspend operator fun invoke(
        cashierId: String = "",
        tableNumber: String = "",
        note: String = ""
    ): Result<Order> {
        return runCatching {
            val now = Clock.System.now()
            val order = Order(
                id = uuid4().toString(),
                items = emptyList(),
                status = OrderStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
                note = note,
                tableNumber = tableNumber,
                cashierId = cashierId
            )
            orderRepository.save(order)
            order
        }
    }
}

/**
 * Computes order totals.
 * Returns a [TotalBreakdown] — a pure calculation, no side effects.
 */
class CalculateOrderTotalUseCase {
    data class TotalBreakdown(
        val subtotal: Double,
        val taxAmount: Double,
        val discountAmount: Double,
        val total: Double,
        val taxRate: Double
    )

    operator fun invoke(
        order: Order,
        taxRate: Double = Order.TAX_RATE,
        extraDiscount: Double = 0.0
    ): TotalBreakdown {
        val subtotal = order.subtotal
        val taxAmount = subtotal * taxRate
        val total = (subtotal + taxAmount - extraDiscount).coerceAtLeast(0.0)
        return TotalBreakdown(
            subtotal = subtotal,
            taxAmount = taxAmount,
            discountAmount = extraDiscount,
            total = total,
            taxRate = taxRate
        )
    }
}
