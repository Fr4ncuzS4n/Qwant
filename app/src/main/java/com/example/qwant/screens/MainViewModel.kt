package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val checking: Boolean = true,
    val online: Boolean = false,
)

/** Проверка доступности сервера на стартовом экране. */
class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        check()
    }

    fun check() {
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            val online = runCatching { QwantApi.health().status == "ok" }.getOrDefault(false)
            _state.update { it.copy(checking = false, online = online) }
        }
    }
}
