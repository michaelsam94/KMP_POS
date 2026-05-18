package com.sahmfood.pos.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.sahmfood.pos.data.local.db.PosDatabase
import com.sahmfood.pos.domain.model.CartItem
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class OrderRepositoryImpl(private val db: PosDatabase) : OrderRepository {
    // SQLDelight generates "orderQueries" from "Order.sq" (both tables in one file)
    private val queries = db.orderQueries

    override fun observeByStatus(status: OrderStatus): Flow<List<Order>> =
        queries.selectOrdersByStatus(status.name)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map {
                    rows ->
                rows.map { loadOrderFromRow(it.id, it.status, it.createdAt, it.updatedAt, it.note, it.tableNumber, it.cashierId) }
            }

    override fun observeAll(): Flow<List<Order>> =
        queries.selectAllOrders()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map {
                    rows ->
                rows.map { loadOrderFromRow(it.id, it.status, it.createdAt, it.updatedAt, it.note, it.tableNumber, it.cashierId) }
            }

    override suspend fun getById(id: String): Order? =
        queries.selectOrderById(id).executeAsOneOrNull()?.let { row ->
            loadOrderFromRow(row.id, row.status, row.createdAt, row.updatedAt, row.note, row.tableNumber, row.cashierId)
        }

    override suspend fun save(order: Order) {
        db.transaction {
            queries.upsertOrder(
                id = order.id,
                status = order.status.name,
                createdAt = order.createdAt.toEpochMilliseconds(),
                updatedAt = order.updatedAt.toEpochMilliseconds(),
                note = order.note,
                tableNumber = order.tableNumber,
                cashierId = order.cashierId,
                isSynced = if (order.status == OrderStatus.SYNCED) 1L else 0L,
            )
            queries.deleteItemsByOrder(order.id)
            order.items.forEach { cartItem ->
                queries.upsertOrderItem(
                    id = uuid4().toString(),
                    orderId = order.id,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    barcode = cartItem.product.barcode,
                    unitPrice = cartItem.unitPrice,
                    discount = cartItem.discount,
                    quantity = cartItem.quantity.toLong(),
                )
            }
        }
    }

    override suspend fun updateStatus(
        orderId: String,
        status: OrderStatus,
    ) {
        queries.updateOrderStatus(
            status = status.name,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = orderId,
        )
    }

    override suspend fun delete(orderId: String) {
        queries.deleteOrder(orderId)
    }

    override suspend fun getPendingSync(): List<Order> =
        queries.selectPendingSync().executeAsList().map { row ->
            loadOrderFromRow(row.id, row.status, row.createdAt, row.updatedAt, row.note, row.tableNumber, row.cashierId)
        }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun loadOrderFromRow(
        id: String,
        status: String,
        createdAt: Long,
        updatedAt: Long,
        note: String,
        tableNumber: String,
        cashierId: String,
    ): Order {
        val items =
            queries.selectItemsByOrder(id).executeAsList().map { item ->
                CartItem(
                    product =
                        Product(
                            id = item.productId,
                            barcode = item.barcode,
                            name = item.productName,
                            description = "",
                            price = item.unitPrice,
                            category = "",
                            stockQuantity = 0,
                        ),
                    quantity = item.quantity.toInt(),
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                )
            }
        return Order(
            id = id,
            items = items,
            status = OrderStatus.valueOf(status),
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            note = note,
            tableNumber = tableNumber,
            cashierId = cashierId,
        )
    }
}
