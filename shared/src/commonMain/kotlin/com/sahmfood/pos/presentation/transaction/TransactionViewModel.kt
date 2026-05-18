package com.sahmfood.pos.presentation.transaction

import com.sahmfood.pos.domain.model.Transaction
import com.sahmfood.pos.domain.usecase.GetTransactionHistoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class TransactionViewModel(
    getHistory: GetTransactionHistoryUseCase,
    scope: CoroutineScope
) {
    private val _state = MutableStateFlow(TransactionUiState())
    val state: StateFlow<TransactionUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isLoading = true) }
        getHistory.invoke()
            .onEach { list ->
                _state.update { it.copy(transactions = list, isLoading = false, error = null) }
            }
            .catch { err ->
                _state.update { it.copy(isLoading = false, error = err.message) }
            }
            .launchIn(scope)
    }
}

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalRevenue: Double get() = transactions.filter { it.status.name == "SUCCESS" }.sumOf { it.amount }
    val transactionCount: Int get() = transactions.size
}
