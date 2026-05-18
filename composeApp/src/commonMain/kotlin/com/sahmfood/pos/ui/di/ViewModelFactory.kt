package com.sahmfood.pos.ui.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sahmfood.pos.data.hardware.MockPaymentTerminal
import com.sahmfood.pos.data.hardware.MockReceiptPrinter
import com.sahmfood.pos.domain.usecase.AddItemToCartUseCase
import com.sahmfood.pos.domain.usecase.CalculateOrderTotalUseCase
import com.sahmfood.pos.domain.usecase.CreateOrderUseCase
import com.sahmfood.pos.domain.usecase.GetTransactionHistoryUseCase
import com.sahmfood.pos.domain.usecase.ProcessPaymentUseCase
import com.sahmfood.pos.domain.usecase.RemoveItemFromCartUseCase
import com.sahmfood.pos.domain.usecase.ScanBarcodeUseCase
import com.sahmfood.pos.domain.usecase.UpdateCartItemQuantityUseCase
import com.sahmfood.pos.domain.repository.OrderRepository
import com.sahmfood.pos.presentation.cart.CartViewModel
import com.sahmfood.pos.presentation.order.OrderListViewModel
import com.sahmfood.pos.presentation.payment.PaymentViewModel
import com.sahmfood.pos.presentation.transaction.TransactionViewModel
import org.koin.compose.koinInject

/** Shared cart instance across tabs (mirrors Android activity-scoped ViewModel). */
object CartViewModelStore {
    var instance: CartViewModel? = null
}

@Composable
fun rememberCartViewModel(): CartViewModel {
    val createOrder: CreateOrderUseCase = koinInject()
    val addItem: AddItemToCartUseCase = koinInject()
    val removeItem: RemoveItemFromCartUseCase = koinInject()
    val updateQuantity: UpdateCartItemQuantityUseCase = koinInject()
    val calculateTotal: CalculateOrderTotalUseCase = koinInject()
    val scanBarcode: ScanBarcodeUseCase = koinInject()
    val orderRepository: OrderRepository = koinInject()

    return remember(createOrder, addItem, removeItem, updateQuantity, calculateTotal, scanBarcode, orderRepository) {
        CartViewModelStore.instance ?: CartViewModel(
            createOrder     = createOrder,
            addItem         = addItem,
            removeItem      = removeItem,
            updateQuantity  = updateQuantity,
            calculateTotal  = calculateTotal,
            scanBarcode     = scanBarcode,
            orderRepository = orderRepository,
            scope           = AppViewModelScope.scope
        ).also { CartViewModelStore.instance = it }
    }
}

@Composable
fun rememberPaymentViewModel(): PaymentViewModel {
    val orderRepository: OrderRepository = koinInject()
    val processPayment: ProcessPaymentUseCase = koinInject()
    val calculateTotal: CalculateOrderTotalUseCase = koinInject()
    val receiptPrinter: MockReceiptPrinter = koinInject()
    val paymentTerminal: MockPaymentTerminal = koinInject()

    return remember(orderRepository, processPayment, calculateTotal, receiptPrinter, paymentTerminal) {
        PaymentViewModel(
            orderRepository = orderRepository,
            processPayment  = processPayment,
            calculateTotal  = calculateTotal,
            receiptPrinter  = receiptPrinter,
            paymentTerminal = paymentTerminal,
            scope           = AppViewModelScope.scope
        )
    }
}

@Composable
fun rememberOrderListViewModel(): OrderListViewModel {
    val orderRepository: OrderRepository = koinInject()

    return remember(orderRepository) {
        OrderListViewModel(
            orderRepository = orderRepository,
            scope           = AppViewModelScope.scope
        )
    }
}

@Composable
fun rememberTransactionViewModel(): TransactionViewModel {
    val getHistory: GetTransactionHistoryUseCase = koinInject()

    return remember(getHistory) {
        TransactionViewModel(
            getHistory = getHistory,
            scope      = AppViewModelScope.scope
        )
    }
}
