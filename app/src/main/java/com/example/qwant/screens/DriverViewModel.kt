package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.DeviationTag
import com.example.qwant.data.Order
import com.example.qwant.data.OrderStatus
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val newOrders: List<Order> = emptyList(),
    val myOrders: List<Order> = emptyList(),
    val tags: List<DeviationTag> = emptyList(),
    val busy: Boolean = false,
)

/**
 * Состояние кабинета водителя.
 *
 * Данные опрашиваются периодически, так как сервер считает «светофор»
 * и время исполнения на каждый запрос.
 */
class DriverViewModel(private val driverId: Int) : ViewModel() {

    private val _state = MutableStateFlow(DriverUiState())
    val state: StateFlow<DriverUiState> = _state.asStateFlow()

    init {
        loadTags()
    }

    fun refresh() {
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            runCatching {
                val newOrders = QwantApi.orders(status = OrderStatus.NEW)
                val myOrders = QwantApi.orders(
                    status = OrderStatus.ACCEPTED,
                    driverId = driverId,
                )
                newOrders to myOrders
            }
                .onSuccess { (newOrders, myOrders) ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            newOrders = newOrders,
                            myOrders = myOrders,
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = throwable.message ?: "network",
                        )
                    }
                }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            runCatching { QwantApi.tags() }
                .onSuccess { tags -> _state.update { it.copy(tags = tags) } }
        }
    }

    /** Действие №1 — «Взять в работу». */
    fun accept(orderId: Int, onResult: (Result<Order>) -> Unit = {}) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = runCatching { QwantApi.acceptOrder(orderId, driverId) }
            _state.update { it.copy(busy = false) }
            result.onSuccess { refresh() }
            onResult(result)
        }
    }

    /** Действие №2 — «Выполнено» (при необходимости с быстрым тегом отклонения). */
    fun complete(
        orderId: Int,
        tag: DeviationTag? = null,
        motohours: Double? = null,
        comment: String? = null,
        onResult: (Result<Order>) -> Unit = {},
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = runCatching {
                QwantApi.completeOrder(
                    orderId = orderId,
                    request = QwantApi.CompleteRequest(
                        deviationTagId = tag?.id,
                        comment = comment,
                        motohoursDelta = motohours,
                    ),
                )
            }
            _state.update { it.copy(busy = false) }
            result.onSuccess { refresh() }
            onResult(result)
        }
    }
}
