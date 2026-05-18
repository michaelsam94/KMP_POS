package com.sahmfood.pos.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahmfood.pos.presentation.cart.CartUiState
import com.sahmfood.pos.presentation.cart.CartViewModel
import com.sahmfood.pos.presentation.order.OrderListUiState
import com.sahmfood.pos.presentation.order.OrderListViewModel
import com.sahmfood.pos.presentation.payment.PaymentUiState
import com.sahmfood.pos.presentation.payment.PaymentViewModel
import com.sahmfood.pos.presentation.transaction.TransactionUiState
import com.sahmfood.pos.presentation.transaction.TransactionViewModel
import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.PaymentMethod
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.repository.OrderRepository
import com.sahmfood.pos.domain.usecase.*
import com.sahmfood.pos.data.hardware.MockPaymentTerminal
import com.sahmfood.pos.data.hardware.MockReceiptPrinter
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * AndroidX ViewModel wrappers that bridge KMP ViewModels (which take a CoroutineScope)
 * to the Android lifecycle via [viewModelScope].
 */

class CartAndroidViewModel(
    createOrder: CreateOrderUseCase,
    addItem: AddItemToCartUseCase,
    removeItem: RemoveItemFromCartUseCase,
    updateQuantity: UpdateCartItemQuantityUseCase,
    calculateTotal: CalculateOrderTotalUseCase,
    scanBarcode: ScanBarcodeUseCase,
    orderRepository: OrderRepository
) : ViewModel() {

    private val delegate = CartViewModel(
        createOrder     = createOrder,
        addItem         = addItem,
        removeItem      = removeItem,
        updateQuantity  = updateQuantity,
        calculateTotal  = calculateTotal,
        scanBarcode     = scanBarcode,
        orderRepository = orderRepository,
        scope           = viewModelScope
    )

    val state: StateFlow<CartUiState> = delegate.state

    fun addProductToCart(product: Product, quantity: Int = 1) =
        delegate.addProductToCart(product, quantity)

    fun removeProductFromCart(productId: String) =
        delegate.removeProductFromCart(productId)

    fun updateItemQuantity(productId: String, quantity: Int) =
        delegate.updateItemQuantity(productId, quantity)

    fun onBarcodeScanned(barcode: String) = delegate.onBarcodeScanned(barcode)

    fun setDiscount(discount: Double) = delegate.setDiscount(discount)

    fun confirmOrder(onConfirmed: ((Order) -> Unit)? = null) = delegate.confirmOrder(onConfirmed)

    fun startNewOrder() = delegate.startNewOrder()

    fun clearError() = delegate.clearError()
}

class PaymentAndroidViewModel(
    orderRepository: OrderRepository,
    processPayment: ProcessPaymentUseCase,
    calculateTotal: CalculateOrderTotalUseCase,
    receiptPrinter: MockReceiptPrinter,
    paymentTerminal: MockPaymentTerminal
) : ViewModel() {

    private val delegate = PaymentViewModel(
        orderRepository = orderRepository,
        processPayment  = processPayment,
        calculateTotal  = calculateTotal,
        receiptPrinter  = receiptPrinter,
        paymentTerminal = paymentTerminal,
        scope           = viewModelScope
    )

    val state: StateFlow<PaymentUiState> = delegate.state

    fun loadOrderById(orderId: String) = delegate.loadOrderById(orderId)
    fun loadOrder(order: Order) = delegate.loadOrder(order)
    fun setPaymentMethod(method: PaymentMethod) = delegate.setPaymentMethod(method)
    fun setCardNumber(value: String) = delegate.setCardNumber(value)
    fun setCardExpiry(value: String) = delegate.setCardExpiry(value)
    fun setCardCvv(value: String) = delegate.setCardCvv(value)
    fun setWalletPhone(value: String) = delegate.setWalletPhone(value)
    fun canConfirmPayment(): Boolean = delegate.canConfirmPayment()
    fun processPayment(cashierId: String = "") = delegate.processPayment(cashierId)
    fun clearError() = delegate.clearError()
    fun resetPayment() = delegate.resetPayment()
}

class TransactionAndroidViewModel(
    getHistory: GetTransactionHistoryUseCase
) : ViewModel() {
    private val delegate = TransactionViewModel(getHistory, viewModelScope)
    val state: StateFlow<TransactionUiState> = delegate.state
}

class OrderListAndroidViewModel(
    orderRepository: OrderRepository
) : ViewModel() {
    private val delegate = OrderListViewModel(orderRepository, viewModelScope)
    val state: StateFlow<OrderListUiState> = delegate.state
    fun filterByStatus(status: com.sahmfood.pos.domain.model.OrderStatus?) =
        delegate.filterByStatus(status)
}

val androidViewModelModule = module {
    viewModel {
        CartAndroidViewModel(
            createOrder     = get(),
            addItem         = get(),
            removeItem      = get(),
            updateQuantity  = get(),
            calculateTotal  = get(),
            scanBarcode     = get(),
            orderRepository = get()
        )
    }
    viewModel {
        PaymentAndroidViewModel(
            orderRepository = get(),
            processPayment  = get(),
            calculateTotal  = get(),
            receiptPrinter  = get(),
            paymentTerminal = get()
        )
    }
    viewModel { TransactionAndroidViewModel(getHistory = get()) }
    viewModel { OrderListAndroidViewModel(orderRepository = get()) }
}
