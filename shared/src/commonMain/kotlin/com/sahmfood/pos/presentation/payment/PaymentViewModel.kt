package com.sahmfood.pos.presentation.payment

import com.sahmfood.pos.data.hardware.MockPaymentTerminal
import com.sahmfood.pos.data.hardware.MockReceiptPrinter
import com.sahmfood.pos.data.hardware.PrintResult
import com.sahmfood.pos.data.hardware.TerminalResult
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.PaymentMethod
import com.sahmfood.pos.domain.model.Transaction
import com.sahmfood.pos.domain.repository.OrderRepository
import com.sahmfood.pos.domain.usecase.CalculateOrderTotalUseCase
import com.sahmfood.pos.domain.usecase.ProcessPaymentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val orderRepository: OrderRepository,
    private val processPayment: ProcessPaymentUseCase,
    private val calculateTotal: CalculateOrderTotalUseCase,
    private val receiptPrinter: MockReceiptPrinter,
    private val paymentTerminal: MockPaymentTerminal,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    fun loadOrderById(
        orderId: String,
        extraDiscount: Double = 0.0,
    ) {
        if (orderId.isBlank()) {
            _state.update { it.copy(error = "Invalid order", isLoading = false) }
            return
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val order = orderRepository.getById(orderId)
            if (order == null) {
                _state.update {
                    it.copy(isLoading = false, error = "Order not found. Go back and try again.")
                }
                return@launch
            }
            if (order.status == OrderStatus.PAID) {
                _state.update {
                    it.copy(isLoading = false, error = "This order is already paid.")
                }
                return@launch
            }
            applyOrder(order, extraDiscount)
        }
    }

    fun loadOrder(
        order: Order,
        taxRate: Double = Order.TAX_RATE,
        discount: Double = 0.0,
    ) {
        applyOrder(order, discount, taxRate)
    }

    private fun applyOrder(
        order: Order,
        discount: Double,
        taxRate: Double = Order.TAX_RATE,
    ) {
        val breakdown = calculateTotal(order, taxRate, discount)
        _state.update {
            it.copy(
                order = order,
                breakdown = breakdown,
                amountTendered = breakdown.total,
                isLoading = false,
                error = null,
            )
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _state.update { current ->
            val total = current.breakdown?.total ?: 0.0
            current.copy(
                paymentMethod = method,
                amountTendered = total,
                cardNumber = "",
                cardExpiry = "",
                cardCvv = "",
                walletPhone = "",
                error = null,
            )
        }
    }

    fun setCardNumber(value: String) = _state.update { it.copy(cardNumber = value.filter { c -> c.isDigit() }.take(16)) }

    fun setCardExpiry(value: String) = _state.update { it.copy(cardExpiry = value.filter { c -> c.isDigit() }.take(4)) }

    fun setCardCvv(value: String) = _state.update { it.copy(cardCvv = value.filter { c -> c.isDigit() }.take(4)) }

    fun setWalletPhone(value: String) = _state.update { it.copy(walletPhone = value.filter { c -> c.isDigit() || c == '+' }.take(15)) }

    fun canConfirmPayment(): Boolean {
        val s = _state.value
        if (s.isLoading || s.isProcessing || s.order == null || s.breakdown == null) return false
        return when (s.paymentMethod) {
            PaymentMethod.CASH -> true
            PaymentMethod.CARD ->
                s.cardNumber.length == 16 &&
                    s.cardExpiry.length == 4 &&
                    s.cardCvv.length >= 3
            PaymentMethod.MOBILE_WALLET -> s.walletPhone.filter { it.isDigit() }.length >= 9
            PaymentMethod.SPLIT -> false
        }
    }

    fun processPayment(cashierId: String = "") {
        val s = _state.value
        val order = s.order
        val breakdown = s.breakdown
        if (order == null || breakdown == null) {
            _state.update { it.copy(error = "Order not loaded") }
            return
        }
        if (!canConfirmPayment()) {
            _state.update { it.copy(error = validationMessage(s)) }
            return
        }

        scope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }

            val amountToCharge = breakdown.total
            val reference =
                when (s.paymentMethod) {
                    PaymentMethod.CASH -> ""
                    PaymentMethod.CARD -> "CARD-${s.cardNumber.takeLast(4)}"
                    PaymentMethod.MOBILE_WALLET -> s.walletPhone.filter { it.isDigit() }
                    PaymentMethod.SPLIT -> ""
                }

            if (s.paymentMethod != PaymentMethod.CASH) {
                val terminalResult =
                    when (s.paymentMethod) {
                        PaymentMethod.CARD -> paymentTerminal.chargeCard(amountToCharge, order.id)
                        PaymentMethod.MOBILE_WALLET ->
                            paymentTerminal.chargeMobileWallet(
                                amountToCharge,
                                s.walletPhone,
                            )
                        else -> TerminalResult.Approved("CASH", "")
                    }

                if (terminalResult is TerminalResult.Declined || terminalResult is TerminalResult.Error) {
                    val reason =
                        when (terminalResult) {
                            is TerminalResult.Declined -> terminalResult.reason
                            is TerminalResult.Error -> terminalResult.message
                            else -> "Payment declined"
                        }
                    _state.update { it.copy(isProcessing = false, error = reason) }
                    return@launch
                }

                val terminalRef = (terminalResult as? TerminalResult.Approved)?.referenceNumber ?: reference
                _state.update { it.copy(referenceNumber = terminalRef) }
            }

            val input =
                ProcessPaymentUseCase.PaymentInput(
                    order = order,
                    paymentMethod = s.paymentMethod,
                    amountTendered = amountToCharge,
                    taxRate = breakdown.taxRate,
                    extraDiscount = breakdown.discountAmount,
                    cashierId = cashierId,
                    referenceNumber = _state.value.referenceNumber.ifBlank { reference },
                )

            processPayment(input)
                .onSuccess { transaction ->
                    val printResult = receiptPrinter.print(order, transaction)
                    val receiptText = (printResult as? PrintResult.Success)?.receiptText
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            transaction = transaction,
                            receiptText = receiptText,
                            paymentComplete = true,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isProcessing = false, error = err.message ?: "Payment failed") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun resetPayment() = _state.update { PaymentUiState() }

    private fun validationMessage(s: PaymentUiState): String =
        when (s.paymentMethod) {
            PaymentMethod.CASH -> "Unable to process cash payment"
            PaymentMethod.CARD ->
                when {
                    s.cardNumber.length != 16 -> "Enter a valid 16-digit card number"
                    s.cardExpiry.length != 4 -> "Enter expiry as MMYY"
                    s.cardCvv.length < 3 -> "Enter a valid CVV"
                    else -> "Complete card details"
                }
            PaymentMethod.MOBILE_WALLET -> "Enter a valid phone number (at least 9 digits)"
            PaymentMethod.SPLIT -> "Split payment is not supported"
        }
}

data class PaymentUiState(
    val order: Order? = null,
    val breakdown: CalculateOrderTotalUseCase.TotalBreakdown? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountTendered: Double = 0.0,
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val walletPhone: String = "",
    val referenceNumber: String = "",
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val paymentComplete: Boolean = false,
    val transaction: Transaction? = null,
    val receiptText: String? = null,
    val error: String? = null,
)
