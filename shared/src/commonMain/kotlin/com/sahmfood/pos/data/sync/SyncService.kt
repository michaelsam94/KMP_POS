package com.sahmfood.pos.data.sync

import com.sahmfood.pos.domain.model.SyncQueueItem
import com.sahmfood.pos.domain.model.SyncStatus
import com.sahmfood.pos.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Outbox-pattern sync service.
 *
 * Strategy:
 *   1. All mutations are persisted locally first.
 *   2. A SyncQueueItem is enqueued for each write.
 *   3. This service drains the queue in order, retrying with exponential back-off.
 *   4. After [SyncQueueItem.MAX_RETRIES] failures the item is ABANDONED and logged.
 *
 * Conflict resolution: last-write-wins with server timestamp.
 * The server is considered authoritative; local records are only overwritten
 * on a subsequent GET (pull) after a successful push.
 */
class SyncService(
    private val syncRepository: SyncRepository,
    private val remoteApi: RemoteApi,
    private val scope: CoroutineScope,
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun start() {
        scope.launch {
            while (true) {
                processPendingQueue()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    suspend fun syncNow() {
        processPendingQueue()
    }

    private suspend fun processPendingQueue() {
        val pending = syncRepository.getPendingItems()
        if (pending.isEmpty()) {
            _syncState.value = SyncState.Idle
            return
        }

        _syncState.value = SyncState.Syncing(pending.size)

        var successCount = 0
        var failCount = 0

        pending.forEach { item ->
            if (!item.canRetry) {
                syncRepository.updateStatus(item.id, SyncStatus.ABANDONED, "Max retries exceeded")
                failCount++
                return@forEach
            }

            syncRepository.updateStatus(item.id, SyncStatus.IN_PROGRESS)
            val success = runCatching { remoteApi.upload(item) }.getOrElse { false }

            if (success) {
                syncRepository.updateStatus(item.id, SyncStatus.SUCCESS)
                successCount++
            } else {
                syncRepository.incrementRetry(item.id)
                val backoffMs = INITIAL_BACKOFF_MS * (1L shl item.retryCount.coerceAtMost(6))
                syncRepository.updateStatus(item.id, SyncStatus.FAILED, "Upload failed, retry in ${backoffMs}ms")
                failCount++
            }
        }

        syncRepository.deleteSucceeded()
        _syncState.value = SyncState.Done(successCount = successCount, failCount = failCount)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
        private const val INITIAL_BACKOFF_MS = 2_000L
    }
}

sealed class SyncState {
    object Idle : SyncState()

    data class Syncing(val pendingCount: Int) : SyncState()

    data class Done(val successCount: Int, val failCount: Int) : SyncState()

    data class Error(val message: String) : SyncState()
}
