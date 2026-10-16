package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.Driver
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverSelectUiState(
    val loading: Boolean = true,
    val drivers: List<Driver> = emptyList(),
    val error: String? = null,
)

class DriverSelectViewModel : ViewModel() {

    private val _state = MutableStateFlow(DriverSelectUiState())
    val state: StateFlow<DriverSelectUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { QwantApi.drivers() }
                .onSuccess { drivers ->
                    _state.update { it.copy(loading = false, drivers = drivers) }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, error = throwable.message ?: "network")
                    }
                }
        }
    }
}
