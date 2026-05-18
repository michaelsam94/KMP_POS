package com.sahmfood.pos.domain.model

import kotlinx.datetime.Instant

/**
 * Immutable record of a completed payment event.
 * Created once after an order is successfully paid.
 */
data class Transaction(
    val id: String,
    val orderId: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val status: TransactionStatus,
    val paidAt: Instant,
    val receiptNumber: String,
    val cashierId: String = "",
    val change: Double = 0.0, // cash tendered minus amount
    val referenceNumber: String = "", // card / mobile ref
    val isSynced: Boolean = false,
) {
    init {
        require(amount > 0.0) { "Transaction amount must be positive" }
        require(change >= 0.0) { "Change must be non-negative" }
    }
}

enum class PaymentMethod {
    CASH,
    CARD,
    MOBILE_WALLET,
    SPLIT,
}

enum class TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
}
