package com.sahmfood.pos.domain.model

import kotlinx.datetime.Instant

/** A pending upload that must be retried until it succeeds or is abandoned. */
data class SyncQueueItem(
    val id: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val payload: String,          // JSON serialised entity
    val status: SyncStatus,
    val retryCount: Int = 0,
    val createdAt: Instant,
    val lastAttemptAt: Instant? = null,
    val errorMessage: String? = null
) {
    val canRetry: Boolean get() = retryCount < MAX_RETRIES

    companion object {
        const val MAX_RETRIES = 5
    }
}

enum class SyncEntityType { ORDER, TRANSACTION }

enum class SyncStatus { PENDING, IN_PROGRESS, SUCCESS, FAILED, ABANDONED }
