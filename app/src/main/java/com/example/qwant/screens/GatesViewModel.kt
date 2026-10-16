package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Gate
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GatesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val gates: List<Gate> = emptyList(),
    val busyGateId: Int? = null,
)

/** Дистанционное управление воротами корпуса. */
class GatesViewModel : ViewModel() {

    private val _state = MutableStateFlow(GatesUiState())
    val state: StateFlow<GatesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { QwantApi.gates() }
                .onSuccess { gates -> _state.update { it.copy(loading = false, gates = gates) } }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = throwable.message ?: "network")
                    }
                }
        }
    }

    fun open(id: Int) = change(id) { QwantApi.openGate(id) }

    fun close(id: Int) = change(id) { QwantApi.closeGate(id) }

    private fun change(id: Int, action: suspend () -> Gate) {
        if (_state.value.busyGateId != null) return
        _state.update { it.copy(busyGateId = id) }
        viewModelScope.launch {
            val result = runCatching { action() }
            _state.update { current ->
                current.copy(
                    busyGateId = null,
                    gates = result.getOrNull()?.let { updated ->
                        current.gates.map { if (it.id == updated.id) updated else it }
                    } ?: current.gates,
                )
            }
        }
    }
}
