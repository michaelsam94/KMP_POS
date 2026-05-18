package com.sahmfood.pos.data.sync

import com.sahmfood.pos.domain.model.SyncQueueItem
import kotlinx.coroutines.delay

/** Contract for the remote back-end API used during sync. */
interface RemoteApi {
    /** Returns true if the upload was accepted by the server. */
    suspend fun upload(item: SyncQueueItem): Boolean
}

/**
 * Mock implementation that always succeeds after a short delay.
 * Replace with a real Ktor-based implementation for production.
 */
class MockRemoteApi : RemoteApi {
    override suspend fun upload(item: SyncQueueItem): Boolean {
        delay(300) // simulate network round-trip
        return true
    }
}
