package com.sahmfood.pos.data.hardware

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/** Contract for card/mobile payment terminal hardware. */
interface PaymentTerminal {
    suspend fun chargeCard(
        amount: Double,
        description: String,
    ): TerminalResult

    suspend fun chargeMobileWallet(
        amount: Double,
        walletType: String,
    ): TerminalResult
}

sealed class TerminalResult {
    data class Approved(val referenceNumber: String, val authCode: String) : TerminalResult()

    data class Declined(val reason: String) : TerminalResult()

    data class Error(val message: String) : TerminalResult()
}

/**
 * Mock payment terminal.
 * Simulates a 1-second network round-trip and always approves amounts > 0.
 */
class MockPaymentTerminal : PaymentTerminal {
    override suspend fun chargeCard(
        amount: Double,
        description: String,
    ): TerminalResult {
        delay(1_000) // simulate POS-to-acquirer round-trip
        return if (amount > 0) {
            TerminalResult.Approved(
                referenceNumber = "REF${Clock.System.now().toEpochMilliseconds()}",
                authCode = "AUTH${(100_000..999_999).random()}",
            )
        } else {
            TerminalResult.Declined("Invalid amount")
        }
    }

    override suspend fun chargeMobileWallet(
        amount: Double,
        walletType: String,
    ): TerminalResult {
        delay(800)
        return TerminalResult.Approved(
            referenceNumber = "$walletType-${Clock.System.now().toEpochMilliseconds()}",
            authCode = "WALLET-OK",
        )
    }
}
