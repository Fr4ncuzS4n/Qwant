package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Building
import com.example.qwant.data.Gate
import com.example.qwant.data.QwantApi
import com.example.qwant.data.Scheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchemeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Список корпусов цеха для выбора; пуст, пока не загрузился. */
    val buildings: List<Building> = emptyList(),
    /** Идентификатор выбранного корпуса. */
    val buildingId: String? = null,
    val scheme: Scheme? = null,
    val selectedElectrolyzerId: Int? = null,
    val busyGateId: Int? = null,
)

/**
 * Схема корпуса: электролизёры (ряды/половины), дороги, опасные зоны, тупики,
 * ворота. Экран показывает **весь корпус целиком**.
 *
 * Список корпусов загружается первым — селектор корпуса появляется сразу, ещё
 * до схемы. Схема каждого корпуса кэшируется, поэтому переключение между уже
 * открытыми корпусами мгновенное (без обращения к сети). Вызовы API вынесены
 * в параметры конструктора — так ViewModel тестируется без сети
 * (см. `SchemeViewModelTest`).
 */
class SchemeViewModel(
    private val buildingsCall: suspend () -> List<Building> = { QwantApi.buildings() },
    private val schemeCall: suspend (String) -> Scheme = { QwantApi.scheme(it) },
    private val gateCall: suspend (Gate) -> Gate = { gate ->
        if (gate.isOpen) QwantApi.closeGate(gate.id) else QwantApi.openGate(gate.id)
    },
) : ViewModel() {

    private val _state = MutableStateFlow(SchemeUiState())
    val state: StateFlow<SchemeUiState> = _state.asStateFlow()

    /** Кэш схем по корпусам: повторное открытие корпуса не ходит в сеть. */
    private val schemes = mutableMapOf<String, Scheme>()

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
                    _state.update { it.copy(loading = false, error = throwable.message ?: "network") }
                }
        }
    }

    /**
     * Смена корпуса. Если схема корпуса уже загружалась — показываем её сразу
     * из кэша, иначе подгружаем с сервера и кладём в кэш.
     */
    fun selectBuilding(id: String) {
        if (id == _state.value.buildingId) return
        val cached = schemes[id]
        _state.update {
            it.copy(
                buildingId = id,
                scheme = cached,
                loading = cached == null,
                selectedElectrolyzerId = null,
                error = null,
            )
        }
        if (cached == null) loadScheme(id)
    }

    /** Схема корпуса `id` из кэша или с сервера; состояние обновляется, только если корпус ещё выбран. */
    private fun loadScheme(id: String) {
        schemes[id]?.let { cached ->
            _state.update { if (it.buildingId == id) it.copy(loading = false, scheme = cached) else it }
            return
        }
        viewModelScope.launch {
            runCatching { schemeCall(id) }
                .onSuccess { scheme ->
                    schemes[id] = scheme
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

    /** Выбранный корпус: ранее выбранный, иначе корпус по умолчанию, иначе первый. */
    private fun chooseBuilding(buildings: List<Building>): String? =
        _state.value.buildingId?.takeIf { current -> buildings.any { it.id == current } }
            ?: buildings.firstOrNull { it.isDefault }?.id
            ?: buildings.firstOrNull()?.id

    fun selectElectrolyzer(id: Int) {
        _state.update {
            it.copy(selectedElectrolyzerId = if (it.selectedElectrolyzerId == id) null else id)
        }
    }

    /** Дистанционное открытие/закрытие ворот корпуса. */
    fun toggleGate(gate: Gate) {
        if (_state.value.busyGateId != null) return
        _state.update { it.copy(busyGateId = gate.id) }
        viewModelScope.launch {
            val result = runCatching { gateCall(gate) }
            _state.update { it.copy(busyGateId = null) }
            result.onSuccess { updated ->
                _state.update { current ->
                    val scheme = current.scheme ?: return@update current
                    val refreshed = scheme.copy(
                        gates = scheme.gates.map { if (it.id == updated.id) updated else it }
                    )
                    schemes[refreshed.building] = refreshed
                    current.copy(scheme = refreshed)
                }
            }
        }
    }
}
