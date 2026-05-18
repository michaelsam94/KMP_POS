package com.sahmfood.pos.domain.repository

import com.sahmfood.pos.domain.model.SyncQueueItem
import com.sahmfood.pos.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/** Contract for the offline-sync outbox. */
interface SyncRepository {
    fun observePending(): Flow<List<SyncQueueItem>>

    suspend fun enqueue(item: SyncQueueItem)

    suspend fun updateStatus(
        itemId: String,
        status: SyncStatus,
        errorMessage: String? = null,
    )

    suspend fun incrementRetry(itemId: String)

    suspend fun getPendingItems(): List<SyncQueueItem>

    suspend fun deleteSucceeded()
}
