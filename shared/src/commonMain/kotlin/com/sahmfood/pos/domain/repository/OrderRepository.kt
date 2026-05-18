package com.sahmfood.pos.domain.repository

import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

/** Contract for order lifecycle persistence. */
interface OrderRepository {
    fun observeByStatus(status: OrderStatus): Flow<List<Order>>

    fun observeAll(): Flow<List<Order>>

    suspend fun getById(id: String): Order?

    /** Persists a new order or replaces an existing one by id. */
    suspend fun save(order: Order)

    suspend fun updateStatus(
        orderId: String,
        status: OrderStatus,
    )

    suspend fun delete(orderId: String)

    /** Returns unsynced orders ready for upload. */
    suspend fun getPendingSync(): List<Order>
}
