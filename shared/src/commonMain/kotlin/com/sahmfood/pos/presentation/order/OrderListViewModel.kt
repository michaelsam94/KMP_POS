package com.sahmfood.pos.presentation.order

import com.sahmfood.pos.domain.model.Order
import com.sahmfood.pos.domain.model.OrderStatus
import com.sahmfood.pos.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class OrderListViewModel(
    private val orderRepository: OrderRepository,
    scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(OrderListUiState())
    val state: StateFlow<OrderListUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isLoading = true) }
        orderRepository.observeAll()
            .onEach { orders ->
                _state.update { it.copy(orders = orders, isLoading = false, error = null) }
            }
            .catch { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
            .launchIn(scope)
    }

    fun filterByStatus(status: OrderStatus?) {
        _state.update { it.copy(filterStatus = status) }
    }
}

data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val filterStatus: OrderStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val filteredOrders: List<Order>
        get() = if (filterStatus == null) orders else orders.filter { it.status == filterStatus }
}
