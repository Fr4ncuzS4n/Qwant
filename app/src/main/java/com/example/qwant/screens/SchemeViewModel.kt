package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Gate
import com.example.qwant.data.Scheme
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchemeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val scheme: Scheme? = null,
    val selectedElectrolyzerId: Int? = null,
    val busyGateId: Int? = null,
)

/** Схема корпуса: электролизёры (ряды/половины), опасные зоны, тупики, ворота. */
class SchemeViewModel : ViewModel() {

    private val _state = MutableStateFlow(SchemeUiState())
    val state: StateFlow<SchemeUiState> = _state.asStateFlow()

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
            val result = runCatching {
                if (gate.isOpen) QwantApi.closeGate(gate.id) else QwantApi.openGate(gate.id)
            }
            _state.update { it.copy(busyGateId = null) }
            result.onSuccess { updated ->
                _state.update { current ->
                    val scheme = current.scheme ?: return@update current
                    current.copy(
                        scheme = scheme.copy(
                            gates = scheme.gates.map { if (it.id == updated.id) updated else it }
                        )
                    )
                }
            }
        }
    }
}
