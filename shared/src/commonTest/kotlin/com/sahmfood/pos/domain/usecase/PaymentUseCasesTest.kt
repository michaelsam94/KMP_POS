package com.sahmfood.pos.domain.usecase

import com.sahmfood.pos.domain.model.CartItem
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.PaymentMethod
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.model.Transaction
import com.sahmfood.pos.domain.model.TransactionStatus
import com.sahmfood.pos.domain.repository.OrderRepository
import com.sahmfood.pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentUseCasesTest {

    private val product = Product(
        id = "p1", barcode = "001", name = "Latte", description = "",
        price = 20.0, category = "Drinks", stockQuantity = 50
    )

    private fun confirmedOrder() = Order(
        id = "o1",
        items = listOf(CartItem(product, 2)),  // subtotal = 40.0
        status = OrderStatus.CONFIRMED,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    private val savedTransactions = mutableMapOf<String, Transaction>()
    private val savedOrders = mutableMapOf<String, Order>()

    private val fakeOrderRepo = object : OrderRepository {
        override fun observeByStatus(status: OrderStatus): Flow<List<Order>> = emptyFlow()
        override fun observeAll(): Flow<List<Order>> = emptyFlow()
        override suspend fun getById(id: String) = savedOrders[id]
        override suspend fun save(order: Order) { savedOrders[order.id] = order }
        override suspend fun updateStatus(orderId: String, status: OrderStatus) {
            savedOrders[orderId] = (savedOrders[orderId] ?: return).copy(status = status)
        }
        override suspend fun delete(orderId: String) {}
        override suspend fun getPendingSync() = emptyList<Order>()
    }

    private val fakeTransactionRepo = object : TransactionRepository {
        override fun observeAll(): Flow<List<Transaction>> = emptyFlow()
        override suspend fun getById(id: String) = savedTransactions[id]
        override suspend fun getByOrderId(orderId: String) =
            savedTransactions.values.firstOrNull { it.orderId == orderId }
        override suspend fun save(transaction: Transaction) {
            savedTransactions[transaction.id] = transaction
        }
        override suspend fun markSynced(transactionId: String) {}
        override suspend fun getPendingSync() = emptyList<Transaction>()
    }

    private fun buildUseCase() = ProcessPaymentUseCase(
        orderRepository       = fakeOrderRepo,
        transactionRepository = fakeTransactionRepo,
        calculateTotal        = CalculateOrderTotalUseCase()
    )

    @Test
    fun `processPayment - creates transaction with correct amount and change`() = runTest {
        val order = confirmedOrder().also { fakeOrderRepo.save(it) }
        val useCase = buildUseCase()

        // subtotal=40, tax=6, total=46; tendered=50 → change=4
        val result = useCase(
            ProcessPaymentUseCase.PaymentInput(
                order           = order,
                paymentMethod   = PaymentMethod.CASH,
                amountTendered  = 50.0,
                taxRate         = 0.15,
                cashierId       = "cashier-99"
            )
        )

        assertTrue(result.isSuccess)
        val tx = result.getOrThrow()
        assertEquals(46.0, tx.amount)
        assertEquals(4.0,  tx.change)
        assertEquals(TransactionStatus.SUCCESS, tx.status)
        assertEquals(PaymentMethod.CASH, tx.paymentMethod)
        assertEquals("cashier-99", tx.cashierId)
    }

    @Test
    fun `processPayment - marks order as PAID`() = runTest {
        val order = confirmedOrder().also { fakeOrderRepo.save(it) }
        val useCase = buildUseCase()

        useCase(
            ProcessPaymentUseCase.PaymentInput(
                order = order, paymentMethod = PaymentMethod.CARD, amountTendered = 46.0
            )
        )

        assertEquals(OrderStatus.PAID, savedOrders[order.id]?.status)
    }

    @Test
    fun `processPayment - fails when tendered amount is insufficient`() = runTest {
        val order = confirmedOrder().also { fakeOrderRepo.save(it) }
        val useCase = buildUseCase()

        val result = useCase(
            ProcessPaymentUseCase.PaymentInput(
                order = order, paymentMethod = PaymentMethod.CASH, amountTendered = 10.0
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `processPayment - fails on empty order`() = runTest {
        val emptyOrder = Order(
            id = "o2", items = emptyList(), status = OrderStatus.CONFIRMED,
            createdAt = Clock.System.now(), updatedAt = Clock.System.now()
        ).also { fakeOrderRepo.save(it) }
        val useCase = buildUseCase()

        val result = useCase(
            ProcessPaymentUseCase.PaymentInput(
                order = emptyOrder, paymentMethod = PaymentMethod.CASH, amountTendered = 0.0
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `processPayment - discount reduces total correctly`() = runTest {
        val order = confirmedOrder().also { fakeOrderRepo.save(it) }
        val useCase = buildUseCase()

        // total without discount = 46; with 6 discount = 40
        val result = useCase(
            ProcessPaymentUseCase.PaymentInput(
                order = order, paymentMethod = PaymentMethod.CASH,
                amountTendered = 40.0, extraDiscount = 6.0
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(40.0, result.getOrThrow().amount)
    }
}
