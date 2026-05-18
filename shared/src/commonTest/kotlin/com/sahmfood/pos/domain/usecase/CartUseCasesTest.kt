package com.sahmfood.pos.domain.usecase

import com.sahmfood.pos.domain.model.CartItem
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD-style tests for cart use cases.
 * All tests are written first, then the use cases are implemented to make them pass.
 */
class CartUseCasesTest {

    // ── Fixtures ──────────────────────────────────────────────────────────

    private val coffeeProduct = Product(
        id = "prod-1", barcode = "111", name = "Espresso",
        description = "Double shot", price = 15.0,
        category = "Beverages", stockQuantity = 100
    )

    private val sandwichProduct = Product(
        id = "prod-2", barcode = "222", name = "Club Sandwich",
        description = "Classic", price = 45.0,
        category = "Food", stockQuantity = 50
    )

    private fun emptyOrder() = Order(
        id = "order-1", items = emptyList(),
        status = OrderStatus.DRAFT,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private val fakeRepo = object : OrderRepository {
        private val orders = mutableMapOf<String, Order>()
        override fun observeByStatus(status: OrderStatus): Flow<List<Order>> = emptyFlow()
        override fun observeAll(): Flow<List<Order>> = emptyFlow()
        override suspend fun getById(id: String) = orders[id]
        override suspend fun save(order: Order) { orders[order.id] = order }
        override suspend fun updateStatus(orderId: String, status: OrderStatus) {
            orders[orderId] = orders[orderId]!!.copy(status = status)
        }
        override suspend fun delete(orderId: String) { orders.remove(orderId) }
        override suspend fun getPendingSync() = emptyList<Order>()
    }

    // ── AddItemToCartUseCase ──────────────────────────────────────────────

    @Test
    fun `addItem - adds new product to empty cart`() = runTest {
        val useCase = AddItemToCartUseCase(fakeRepo)
        val order = emptyOrder().also { fakeRepo.save(it) }

        val result = useCase(order, coffeeProduct, 2)

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(1, updated.items.size)
        assertEquals(coffeeProduct, updated.items.first().product)
        assertEquals(2, updated.items.first().quantity)
    }

    @Test
    fun `addItem - increments quantity for existing product`() = runTest {
        val useCase = AddItemToCartUseCase(fakeRepo)
        val orderWithCoffee = emptyOrder().copy(
            items = listOf(CartItem(coffeeProduct, quantity = 1))
        ).also { fakeRepo.save(it) }

        val result = useCase(orderWithCoffee, coffeeProduct, 3)

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrThrow().items.first().quantity)
    }

    @Test
    fun `addItem - fails when quantity is zero`() = runTest {
        val useCase = AddItemToCartUseCase(fakeRepo)
        val order = emptyOrder().also { fakeRepo.save(it) }

        val result = useCase(order, coffeeProduct, 0)

        assertTrue(result.isFailure)
    }

    // ── RemoveItemFromCartUseCase ─────────────────────────────────────────

    @Test
    fun `removeItem - removes correct product`() = runTest {
        val useCase = RemoveItemFromCartUseCase(fakeRepo)
        val order = emptyOrder().copy(
            items = listOf(
                CartItem(coffeeProduct, 1),
                CartItem(sandwichProduct, 2)
            )
        ).also { fakeRepo.save(it) }

        val result = useCase(order, coffeeProduct.id)

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(1, updated.items.size)
        assertEquals(sandwichProduct, updated.items.first().product)
    }

    @Test
    fun `removeItem - removing non-existent product leaves cart unchanged`() = runTest {
        val useCase = RemoveItemFromCartUseCase(fakeRepo)
        val order = emptyOrder().copy(
            items = listOf(CartItem(coffeeProduct, 1))
        ).also { fakeRepo.save(it) }

        val result = useCase(order, "non-existent-id")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().items.size)
    }

    // ── UpdateCartItemQuantityUseCase ─────────────────────────────────────

    @Test
    fun `updateQuantity - updates to new quantity`() = runTest {
        val useCase = UpdateCartItemQuantityUseCase(fakeRepo)
        val order = emptyOrder().copy(
            items = listOf(CartItem(coffeeProduct, 1))
        ).also { fakeRepo.save(it) }

        val result = useCase(order, coffeeProduct.id, 5)

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrThrow().items.first().quantity)
    }

    @Test
    fun `updateQuantity - quantity 0 removes the item`() = runTest {
        val useCase = UpdateCartItemQuantityUseCase(fakeRepo)
        val order = emptyOrder().copy(
            items = listOf(CartItem(coffeeProduct, 3))
        ).also { fakeRepo.save(it) }

        val result = useCase(order, coffeeProduct.id, 0)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().items.isEmpty())
    }

    @Test
    fun `updateQuantity - negative quantity returns failure`() = runTest {
        val useCase = UpdateCartItemQuantityUseCase(fakeRepo)
        val order = emptyOrder().also { fakeRepo.save(it) }

        val result = useCase(order, coffeeProduct.id, -1)

        assertTrue(result.isFailure)
    }

    // ── CalculateOrderTotalUseCase ────────────────────────────────────────

    @Test
    fun `calculateTotal - computes subtotal, tax, and total correctly`() {
        val useCase = CalculateOrderTotalUseCase()
        val order = emptyOrder().copy(
            items = listOf(
                CartItem(coffeeProduct, 2),    // 2 × 15.0 = 30.0
                CartItem(sandwichProduct, 1)   // 1 × 45.0 = 45.0
            )
        )

        val breakdown = useCase(order, taxRate = 0.15, extraDiscount = 5.0)

        assertEquals(75.0, breakdown.subtotal)
        assertEquals(11.25, breakdown.taxAmount)
        assertEquals(5.0, breakdown.discountAmount)
        assertEquals(81.25, breakdown.total)
    }

    @Test
    fun `calculateTotal - total never goes below zero with large discount`() {
        val useCase = CalculateOrderTotalUseCase()
        val order = emptyOrder().copy(items = listOf(CartItem(coffeeProduct, 1)))

        val breakdown = useCase(order, taxRate = 0.15, extraDiscount = 999.0)

        assertEquals(0.0, breakdown.total)
    }

    // ── CreateOrderUseCase ────────────────────────────────────────────────

    @Test
    fun `createOrder - creates order in DRAFT with empty cart`() = runTest {
        val useCase = CreateOrderUseCase(fakeRepo)

        val result = useCase(cashierId = "cashier-1", tableNumber = "T5")

        assertTrue(result.isSuccess)
        val order = result.getOrThrow()
        assertEquals(OrderStatus.DRAFT, order.status)
        assertTrue(order.items.isEmpty())
        assertEquals("cashier-1", order.cashierId)
        assertEquals("T5", order.tableNumber)
    }

    // ── Domain model validation ───────────────────────────────────────────

    @Test
    fun `Product - rejects negative price`() {
        assertFailsWith<IllegalArgumentException> {
            Product("id", "barcode", "Name", "desc", price = -1.0, "cat", 0)
        }
    }

    @Test
    fun `CartItem - rejects quantity less than 1`() {
        assertFailsWith<IllegalArgumentException> {
            CartItem(coffeeProduct, quantity = 0)
        }
    }

    @Test
    fun `CartItem lineTotal - is (price - discount) × quantity`() {
        val item = CartItem(coffeeProduct, quantity = 3, discount = 5.0)
        assertEquals(30.0, item.lineTotal)   // (15 - 5) × 3 = 30
    }
}
