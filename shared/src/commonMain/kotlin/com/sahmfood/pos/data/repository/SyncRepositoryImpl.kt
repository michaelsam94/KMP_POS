package com.sahmfood.pos.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sahmfood.pos.data.local.db.PosDatabase
import com.sahmfood.pos.domain.model.SyncEntityType
import com.sahmfood.pos.domain.model.SyncQueueItem
import com.sahmfood.pos.domain.model.SyncStatus
import com.sahmfood.pos.domain.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SyncRepositoryImpl(db: PosDatabase) : SyncRepository {

    // SQLDelight generates "syncQueueQueries" from "SyncQueue.sq"
    private val queries = db.syncQueueQueries

    override fun observePending(): Flow<List<SyncQueueItem>> =
        queries.selectPending()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun enqueue(item: SyncQueueItem) {
        queries.enqueue(
            id         = item.id,
            entityType = item.entityType.name,
            entityId   = item.entityId,
            payload    = item.payload,
            createdAt  = item.createdAt.toEpochMilliseconds()
        )
    }

    override suspend fun updateStatus(itemId: String, status: SyncStatus, errorMessage: String?) {
        queries.updateStatus(
            status        = status.name,
            errorMessage  = errorMessage,
            lastAttemptAt = Clock.System.now().toEpochMilliseconds(),
            id            = itemId
        )
    }

    override suspend fun incrementRetry(itemId: String) {
        queries.incrementRetry(
            lastAttemptAt = Clock.System.now().toEpochMilliseconds(),
            id            = itemId
        )
    }

    override suspend fun getPendingItems(): List<SyncQueueItem> =
        queries.selectPending().executeAsList().map { it.toDomain() }

    override suspend fun deleteSucceeded() {
        queries.deleteSucceeded()
    }

    // ── Mapper ────────────────────────────────────────────────────────────
    private fun com.sahmfood.pos.SyncQueueEntity.toDomain() = SyncQueueItem(
        id            = id,
        entityType    = SyncEntityType.valueOf(entityType),
        entityId      = entityId,
        payload       = payload,
        status        = SyncStatus.valueOf(status),
        retryCount    = retryCount.toInt(),
        createdAt     = Instant.fromEpochMilliseconds(createdAt),
        lastAttemptAt = lastAttemptAt?.let { Instant.fromEpochMilliseconds(it) },
        errorMessage  = errorMessage
    )
}
