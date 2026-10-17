package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Building
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
    /** Список корпусов цеха: диспетчер выбирает, в какой корпус отправить водителя. */
    val buildings: List<Building> = emptyList(),
    /** Идентификатор выбранного корпуса. */
    val buildingId: String? = null,
    val scheme: Scheme? = null,
    /** Видимый фрагмент корпуса при выборе электролизёра. */
    val fragment: SchemeFragment? = null,
    val type: String = OrderType.OBRABOTKA,
    val electrolyzerId: Int? = null,
    val description: String = "",
    val requestedBy: String = "Диспетчер",
    val submitting: Boolean = false,
) {
    /** Выбранный корпус (для подписи выбранного электролизёра). */
    val building: Building? get() = buildings.firstOrNull { it.id == buildingId }
}

/**
 * Создание заявки диспетчером: **выбор корпуса** (куда отправить водителя),
 * тип заявки и электролизёр на схеме выбранного корпуса.
 *
 * Вызовы API вынесены в параметры конструктора — ViewModel тестируется без сети
 * (см. `CreateOrderViewModelTest`).
 */
class CreateOrderViewModel(
    requesterName: String = "",
    private val buildingsCall: suspend () -> List<Building> = { QwantApi.buildings() },
    private val schemeCall: suspend (String) -> Scheme = { QwantApi.scheme(it) },
    private val createCall: suspend (QwantApi.CreateOrderRequest) -> Order = { QwantApi.createOrder(it) },
) : ViewModel() {

    private val _state = MutableStateFlow(
        CreateOrderUiState(
            requestedBy = requesterName.ifBlank { "Диспетчер" },
        )
    )
    val state: StateFlow<CreateOrderUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Загрузка списка корпусов и схемы корпуса по умолчанию. */
    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { buildingsCall() }
                .onSuccess { buildings ->
                    val id = chooseBuilding(buildings)
                    _state.update { it.copy(buildings = buildings, buildingId = id) }
                    if (id == null) {
                        _state.update { it.copy(loading = false) }
                    } else {
                        loadScheme(id)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = throwable.message ?: "network")
                    }
                }
        }
    }

    /** Смена корпуса: сбрасываем выбранный электролизёр и грузим схему корпуса. */
    fun selectBuilding(id: String) {
        if (id == _state.value.buildingId) return
        _state.update {
            it.copy(
                buildingId = id,
                scheme = null,
                fragment = null,
                electrolyzerId = null,
                loading = true,
                error = null,
            )
        }
        loadScheme(id)
    }

    private fun loadScheme(id: String) {
        viewModelScope.launch {
            runCatching { schemeCall(id) }
                .onSuccess { scheme ->
                    _state.update {
                        if (it.buildingId == id) it.copy(loading = false, scheme = scheme) else it
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        if (it.buildingId == id) {
                            it.copy(loading = false, error = throwable.message ?: "network")
                        } else {
                            it
                        }
                    }
                }
        }
    }

    /** Ранее выбранный корпус, иначе корпус по умолчанию, иначе первый. */
    private fun chooseBuilding(buildings: List<Building>): String? =
        _state.value.buildingId?.takeIf { current -> buildings.any { it.id == current } }
            ?: buildings.firstOrNull { it.isDefault }?.id
            ?: buildings.firstOrNull()?.id

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
                createCall(
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
