package com.sahmfood.pos.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sahmfood.pos.data.local.db.PosDatabase
import com.sahmfood.pos.domain.model.PaymentMethod
import com.sahmfood.pos.domain.model.Transaction
import com.sahmfood.pos.domain.model.TransactionStatus
import com.sahmfood.pos.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class TransactionRepositoryImpl(db: PosDatabase) : TransactionRepository {

    // SQLDelight generates "transactionQueries" from "Transaction.sq"
    private val queries = db.transactionQueries

    override fun observeAll(): Flow<List<Transaction>> =
        queries.selectAllTransactions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): Transaction? =
        queries.selectTransactionById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun getByOrderId(orderId: String): Transaction? =
        queries.selectTransactionByOrderId(orderId).executeAsOneOrNull()?.toDomain()

    override suspend fun save(transaction: Transaction) {
        queries.upsertTransaction(
            id              = transaction.id,
            orderId         = transaction.orderId,
            amount          = transaction.amount,
            paymentMethod   = transaction.paymentMethod.name,
            status          = transaction.status.name,
            paidAt          = transaction.paidAt.toEpochMilliseconds(),
            receiptNumber   = transaction.receiptNumber,
            cashierId       = transaction.cashierId,
            change          = transaction.change,
            referenceNumber = transaction.referenceNumber,
            isSynced        = if (transaction.isSynced) 1L else 0L
        )
    }

    override suspend fun markSynced(transactionId: String) {
        queries.markSynced(transactionId)
    }

    override suspend fun getPendingSync(): List<Transaction> =
        queries.selectPendingSync().executeAsList().map { it.toDomain() }

    // ── Mapper ────────────────────────────────────────────────────────────
    private fun com.sahmfood.pos.TransactionEntity.toDomain() = Transaction(
        id              = id,
        orderId         = orderId,
        amount          = amount,
        paymentMethod   = PaymentMethod.valueOf(paymentMethod),
        status          = TransactionStatus.valueOf(status),
        paidAt          = Instant.fromEpochMilliseconds(paidAt),
        receiptNumber   = receiptNumber,
        cashierId       = cashierId,
        change          = change,
        referenceNumber = referenceNumber,
        isSynced        = isSynced == 1L
    )
}
