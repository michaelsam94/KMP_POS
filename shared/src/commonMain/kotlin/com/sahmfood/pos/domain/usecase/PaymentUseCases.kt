package com.sahmfood.pos.domain.usecase

import com.benasher44.uuid.uuid4
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.PaymentMethod
import com.sahmfood.pos.domain.model.Transaction
import com.sahmfood.pos.domain.model.TransactionStatus
import com.sahmfood.pos.domain.repository.OrderRepository
import com.sahmfood.pos.domain.repository.TransactionRepository
import kotlinx.datetime.Clock

/**
 * Processes a payment for a confirmed order.
 * On success it:
 *   1. Creates a [Transaction] record.
 *   2. Marks the order as [OrderStatus.PAID].
 */
class ProcessPaymentUseCase(
    private val orderRepository: OrderRepository,
    private val transactionRepository: TransactionRepository,
    private val calculateTotal: CalculateOrderTotalUseCase,
) {
    data class PaymentInput(
        val order: Order,
        val paymentMethod: PaymentMethod,
        val amountTendered: Double, // cash given / card charged
        val taxRate: Double = Order.TAX_RATE,
        val extraDiscount: Double = 0.0,
        val cashierId: String = "",
        val referenceNumber: String = "", // card / mobile ref
    )

    suspend operator fun invoke(input: PaymentInput): Result<Transaction> {
        return runCatching {
            require(input.order.status == OrderStatus.CONFIRMED || input.order.status == OrderStatus.DRAFT) {
                "Cannot pay an order with status ${input.order.status}"
            }
            require(input.order.items.isNotEmpty()) { "Cannot pay an empty order" }

            val breakdown = calculateTotal(input.order, input.taxRate, input.extraDiscount)
            require(input.amountTendered >= breakdown.total) {
                "Tendered amount (${input.amountTendered}) is less than total (${breakdown.total})"
            }

            val now = Clock.System.now()
            val receiptNumber = "RCP-${now.toEpochMilliseconds()}"

            val transaction =
                Transaction(
                    id = uuid4().toString(),
                    orderId = input.order.id,
                    amount = breakdown.total,
                    paymentMethod = input.paymentMethod,
                    status = TransactionStatus.SUCCESS,
                    paidAt = now,
                    receiptNumber = receiptNumber,
                    cashierId = input.cashierId,
                    change = input.amountTendered - breakdown.total,
                    referenceNumber = input.referenceNumber,
                    isSynced = false,
                )

            transactionRepository.save(transaction)
            orderRepository.updateStatus(input.order.id, OrderStatus.PAID)
            transaction
        }
    }
}

/** Fetches the full transaction history, ordered by recency. */
class GetTransactionHistoryUseCase(
    private val transactionRepository: TransactionRepository,
) {
    fun invoke() = transactionRepository.observeAll()
}
