package com.sahmfood.pos.ui.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val scope = rememberCoroutineScope()
    val createOrder: CreateOrderUseCase = koinInject()
    val addItem: AddItemToCartUseCase = koinInject()
    val removeItem: RemoveItemFromCartUseCase = koinInject()
    val updateQuantity: UpdateCartItemQuantityUseCase = koinInject()
    val calculateTotal: CalculateOrderTotalUseCase = koinInject()
    val scanBarcode: ScanBarcodeUseCase = koinInject()
    val orderRepository: OrderRepository = koinInject()

    return remember {
        CartViewModelStore.instance ?: CartViewModel(
            createOrder       = createOrder,
            addItem           = addItem,
            removeItem        = removeItem,
            updateQuantity    = updateQuantity,
            calculateTotal    = calculateTotal,
            scanBarcode       = scanBarcode,
            orderRepository   = orderRepository,
            scope             = scope
        ).also { CartViewModelStore.instance = it }
    }
}

@Composable
fun rememberPaymentViewModel(): PaymentViewModel {
    val scope = rememberCoroutineScope()
    val orderRepository: OrderRepository = koinInject()
    val processPayment: ProcessPaymentUseCase = koinInject()
    val calculateTotal: CalculateOrderTotalUseCase = koinInject()
    val receiptPrinter: MockReceiptPrinter = koinInject()
    val paymentTerminal: MockPaymentTerminal = koinInject()

    return remember {
        PaymentViewModel(
            orderRepository = orderRepository,
            processPayment  = processPayment,
            calculateTotal  = calculateTotal,
            receiptPrinter  = receiptPrinter,
            paymentTerminal = paymentTerminal,
            scope           = scope
        )
    }
}

@Composable
fun rememberOrderListViewModel(): OrderListViewModel {
    val scope = rememberCoroutineScope()
    val orderRepository: OrderRepository = koinInject()

    return remember {
        OrderListViewModel(
            orderRepository = orderRepository,
            scope           = scope
        )
    }
}

@Composable
fun rememberTransactionViewModel(): TransactionViewModel {
    val scope = rememberCoroutineScope()
    val getHistory: GetTransactionHistoryUseCase = koinInject()

    return remember {
        TransactionViewModel(
            getHistory = getHistory,
            scope      = scope
        )
    }
}
