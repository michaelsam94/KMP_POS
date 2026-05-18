package com.sahmfood.pos.data.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Contract for barcode/QR scanner hardware or camera. */
interface BarcodeScanner {
    /** Emits barcode strings as they are scanned. */
    fun scanStream(): Flow<ScanEvent>
}

sealed class ScanEvent {
    data class BarcodeDetected(val barcode: String) : ScanEvent()

    data class ScanError(val message: String) : ScanEvent()
}

/**
 * Mock scanner that cycles through a preset list of barcodes.
 * Useful for demo and integration tests without physical hardware.
 */
class MockBarcodeScanner(
    private val mockBarcodes: List<String> = DEFAULT_BARCODES,
) : BarcodeScanner {
    override fun scanStream(): Flow<ScanEvent> =
        flow {
            var index = 0
            while (true) {
                delay(3_000) // simulate a new scan every 3 seconds
                emit(ScanEvent.BarcodeDetected(mockBarcodes[index % mockBarcodes.size]))
                index++
            }
        }

    companion object {
        val DEFAULT_BARCODES =
            listOf(
                "6281007010010",
                "6281007010027",
                "6281007010034",
                "6281007010041",
            )
    }
}
