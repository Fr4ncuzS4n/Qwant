package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Order
import com.example.qwant.data.OrderType
import com.example.qwant.data.QwantApi
import com.example.qwant.data.Scheme
import com.example.qwant.ui.SchemeFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateOrderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val scheme: Scheme? = null,
    /** Видимый фрагмент корпуса при выборе электролизёра. */
    val fragment: SchemeFragment? = null,
    val type: String = OrderType.OBRABOTKA,
    val electrolyzerId: Int? = null,
    val description: String = "",
    val requestedBy: String = "Диспетчер",
    val submitting: Boolean = false,
)

/** Создание заявки диспетчером: тип + электролизёр на схеме корпуса. */
class CreateOrderViewModel(requesterName: String = "") : ViewModel() {

    private val _state = MutableStateFlow(
        CreateOrderUiState(
            requestedBy = requesterName.ifBlank { "Диспетчер" },
        )
    )
    val state: StateFlow<CreateOrderUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { QwantApi.scheme() }
                .onSuccess { scheme -> _state.update { it.copy(loading = false, scheme = scheme) } }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = throwable.message ?: "network")
                    }
                }
        }
    }

    fun showFragment(fragment: SchemeFragment) {
        _state.update { it.copy(fragment = fragment) }
    }

    fun selectType(type: String) {
        _state.update { it.copy(type = type, description = defaultDescription(type)) }
    }

    fun selectElectrolyzer(id: Int) {
        _state.update { it.copy(electrolyzerId = id) }
    }

    fun updateDescription(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun updateRequestedBy(value: String) {
        _state.update { it.copy(requestedBy = value) }
    }

    fun submit(onResult: (Result<Order>) -> Unit) {
        val current = _state.value
        val electrolyzerId = current.electrolyzerId
        if (electrolyzerId == null) {
            onResult(Result.failure(IllegalStateException("electrolyzer")))
            return
        }
        if (current.submitting) return
        _state.update { it.copy(submitting = true) }

        viewModelScope.launch {
            val result = runCatching {
                QwantApi.createOrder(
                    QwantApi.CreateOrderRequest(
                        type = current.type,
                        electrolyzerId = electrolyzerId,
                        description = current.description.takeIf { it.isNotBlank() },
                        requestedBy = current.requestedBy.takeIf { it.isNotBlank() },
                        isRepair = current.type == OrderType.REMONT,
                    )
                )
            }
            _state.update { it.copy(submitting = false) }
            onResult(result)
        }
    }

    private fun defaultDescription(type: String): String = when (type) {
        OrderType.PODSYPKA -> "Подсыпка глинозёма"
        OrderType.REMONT -> "Заявка на ремонт"
        else -> "Обработка корки"
    }
}
