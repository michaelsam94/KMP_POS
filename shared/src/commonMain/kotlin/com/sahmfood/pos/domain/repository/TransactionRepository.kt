package com.sahmfood.pos.domain.repository

import com.sahmfood.pos.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/** Contract for financial transaction persistence. */
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>

    suspend fun getById(id: String): Transaction?

    suspend fun getByOrderId(orderId: String): Transaction?

    /** Persists a new transaction record (idempotent by id). */
    suspend fun save(transaction: Transaction)

    suspend fun markSynced(transactionId: String)

    /** Returns all transactions not yet uploaded to the server. */
    suspend fun getPendingSync(): List<Transaction>
}
