package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Order
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val order: Order? = null,
)

class OrderViewModel(private val orderId: Int) : ViewModel() {

    private val _state = MutableStateFlow(OrderUiState())
    val state: StateFlow<OrderUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { QwantApi.order(orderId) }
                .onSuccess { order -> _state.update { it.copy(loading = false, order = order) } }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = throwable.message ?: "network")
                    }
                }
        }
    }
}
