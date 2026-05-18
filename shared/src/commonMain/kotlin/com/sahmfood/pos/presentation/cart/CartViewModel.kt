package com.sahmfood.pos.presentation.cart

import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.usecase.AddItemToCartUseCase
import com.sahmfood.pos.domain.usecase.CalculateOrderTotalUseCase
import com.sahmfood.pos.domain.usecase.CreateOrderUseCase
import com.sahmfood.pos.domain.usecase.RemoveItemFromCartUseCase
import com.sahmfood.pos.domain.usecase.UpdateCartItemQuantityUseCase
import com.sahmfood.pos.domain.usecase.ScanBarcodeUseCase
import com.sahmfood.pos.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel(
    private val createOrder: CreateOrderUseCase,
    private val addItem: AddItemToCartUseCase,
    private val removeItem: RemoveItemFromCartUseCase,
    private val updateQuantity: UpdateCartItemQuantityUseCase,
    private val calculateTotal: CalculateOrderTotalUseCase,
    private val scanBarcode: ScanBarcodeUseCase,
    private val orderRepository: OrderRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    init {
        startNewOrder()
    }

    /** Clears the cart UI immediately, then creates a fresh empty order in the database. */
    fun startNewOrder(cashierId: String = "", tableNumber: String = "") {
        _state.update {
            CartUiState(
                isLoading = true,
                order = null,
                breakdown = null,
                extraDiscount = 0.0,
                error = null
            )
        }
        scope.launch {
            createOrder(cashierId = cashierId, tableNumber = tableNumber)
                .onSuccess { order ->
                    _state.update {
                        CartUiState(
                            order = order,
                            breakdown = null,
                            extraDiscount = 0.0,
                            isLoading = false
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun addProductToCart(product: Product, quantity: Int = 1) {
        val currentOrder = _state.value.order ?: return
        scope.launch {
            addItem(currentOrder, product, quantity)
                .onSuccess { updatedOrder ->
                    _state.update { it.copy(order = updatedOrder, error = null) }
                    recalculate(updatedOrder)
                }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message) }
                }
        }
    }

    fun removeProductFromCart(productId: String) {
        val currentOrder = _state.value.order ?: return
        scope.launch {
            removeItem(currentOrder, productId)
                .onSuccess { updatedOrder ->
                    _state.update { it.copy(order = updatedOrder, error = null) }
                    recalculate(updatedOrder)
                }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message) }
                }
        }
    }

    fun updateItemQuantity(productId: String, quantity: Int) {
        val currentOrder = _state.value.order ?: return
        scope.launch {
            updateQuantity(currentOrder, productId, quantity)
                .onSuccess { updatedOrder ->
                    _state.update { it.copy(order = updatedOrder, error = null) }
                    recalculate(updatedOrder)
                }
                .onFailure { err ->
                    _state.update { it.copy(error = err.message) }
                }
        }
    }

    fun onBarcodeScanned(barcode: String) {
        scope.launch {
            val product = scanBarcode(barcode)
            if (product != null) {
                addProductToCart(product)
            } else {
                _state.update { it.copy(error = "Product not found for barcode: $barcode") }
            }
        }
    }

    fun setDiscount(discount: Double) {
        _state.update { it.copy(extraDiscount = discount) }
        _state.value.order?.let { recalculate(it) }
    }

    fun confirmOrder(onConfirmed: ((Order) -> Unit)? = null) {
        val currentOrder = _state.value.order ?: return
        scope.launch {
            if (currentOrder.items.isEmpty()) {
                _state.update { it.copy(error = "Cart is empty") }
                return@launch
            }
            orderRepository.updateStatus(currentOrder.id, OrderStatus.CONFIRMED)
            val updated = currentOrder.copy(status = OrderStatus.CONFIRMED)
            _state.update { it.copy(order = updated, error = null) }
            onConfirmed?.invoke(updated)
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun recalculate(order: Order) {
        val breakdown = calculateTotal(order, extraDiscount = _state.value.extraDiscount)
        _state.update { it.copy(breakdown = breakdown) }
    }
}

data class CartUiState(
    val order: Order? = null,
    val breakdown: CalculateOrderTotalUseCase.TotalBreakdown? = null,
    val extraDiscount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)
