package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Order
import com.example.qwant.data.OrderStatus
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Фильтры списка заявок в кабинете диспетчера. */
enum class DispatcherFilter { ALL, NEW, ACCEPTED, DONE, REPAIR }

data class DispatcherUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val filter: DispatcherFilter = DispatcherFilter.ALL,
    val orders: List<Order> = emptyList(),
)

class DispatcherViewModel : ViewModel() {

    private val _state = MutableStateFlow(DispatcherUiState())
    val state: StateFlow<DispatcherUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setFilter(filter: DispatcherFilter) {
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    fun refresh() {
        val filter = _state.value.filter
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            runCatching { QwantApi.orders() }
                .onSuccess { all ->
                    _state.update { it.copy(loading = false, orders = applyFilter(all, filter)) }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = throwable.message ?: "network")
                    }
                }
        }
    }

    private fun applyFilter(orders: List<Order>, filter: DispatcherFilter): List<Order> =
        when (filter) {
            DispatcherFilter.ALL -> orders
            DispatcherFilter.NEW -> orders.filter { it.status == OrderStatus.NEW }
            DispatcherFilter.ACCEPTED -> orders.filter { it.status == OrderStatus.ACCEPTED }
            DispatcherFilter.DONE -> orders.filter { it.status == OrderStatus.DONE }
            DispatcherFilter.REPAIR -> orders.filter { it.isRepair }
        }
}
