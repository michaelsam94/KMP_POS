package com.sahmfood.pos.data.hardware

import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.Transaction
import com.sahmfood.pos.util.toMoneyString
import kotlinx.coroutines.delay

/** Contract for receipt printers (real or mock). */
interface ReceiptPrinter {
    suspend fun print(order: Order, transaction: Transaction): PrintResult
}

sealed class PrintResult {
    data class Success(val receiptText: String) : PrintResult()
    data class Failure(val reason: String) : PrintResult()
}

/**
 * Simulates a thermal receipt printer.
 * Formats a human-readable receipt, waits a short delay to mimic hardware I/O,
 * then returns the formatted text.
 */
class MockReceiptPrinter : ReceiptPrinter {

    override suspend fun print(order: Order, transaction: Transaction): PrintResult {
        delay(400) // simulate ESC/POS communication delay

        val lines = buildList {
            add("========================================")
            add("          SAHM FOOD POS SYSTEM          ")
            add("========================================")
            add("Receipt  : ${transaction.receiptNumber}")
            add("Date     : ${transaction.paidAt}")
            if (order.tableNumber.isNotBlank()) add("Table    : ${order.tableNumber}")
            if (order.cashierId.isNotBlank())   add("Cashier  : ${order.cashierId}")
            add("----------------------------------------")
            order.items.forEach { item ->
                val lineTotal = item.lineTotal.toMoneyString()
                add("${item.product.name.padEnd(24)} x${item.quantity}")
                add("  @ ${item.unitPrice}${if (item.discount > 0) " (-${item.discount})" else ""} = $lineTotal")
            }
            add("----------------------------------------")
            add("Subtotal : ${order.subtotal.toMoneyString()}")
            add("Tax(15%) : ${order.tax(Order.TAX_RATE).toMoneyString()}")
            add("TOTAL    : ${transaction.amount.toMoneyString()}")
            add("Paid     : ${transaction.paymentMethod.name}")
            if (transaction.change > 0)
                add("Change   : ${transaction.change.toMoneyString()}")
            if (transaction.referenceNumber.isNotBlank())
                add("Ref      : ${transaction.referenceNumber}")
            add("========================================")
            add("      Thank you for dining with us!     ")
            add("========================================")
        }

        return PrintResult.Success(lines.joinToString("\n"))
    }
}
