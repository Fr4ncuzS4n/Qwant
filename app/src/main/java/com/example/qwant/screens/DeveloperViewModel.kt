package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.QwantApi
import com.example.qwant.data.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeveloperUiState(
    /** Сохранённый адрес сервера (`ServerConfig`). */
    val baseUrl: String = ServerConfig.baseUrl,
    /** Открыт ли режим редактирования адреса. */
    val editing: Boolean = false,
    /** Значение поля ввода в режиме редактирования. */
    val addressInput: String = ServerConfig.baseUrl,
    val checkingServer: Boolean = false,
    val serverOnline: Boolean = false,
)

/**
 * Состояние экрана «Для разработчика»: адрес сервера (`server/`) и проверка связи.
 *
 * [healthCheck] вынесена в конструктор — ViewModel тестируется без сети и без
 * Android-контекста (см. `DeveloperViewModelTest`). Персистенцию адреса выполняет
 * экран (`ServerConfig.save`), сюда передаётся результат через [save].
 */
class DeveloperViewModel(
    private val healthCheck: suspend () -> Boolean = { QwantApi.health().status == "ok" },
) : ViewModel() {

    private val _state = MutableStateFlow(DeveloperUiState())
    val state: StateFlow<DeveloperUiState> = _state.asStateFlow()

    init {
        checkServer()
    }

    /** Проверка доступности сервера по текущему адресу. */
    fun checkServer() {
        _state.update { it.copy(checkingServer = true) }
        viewModelScope.launch {
            val online = runCatching { healthCheck() }.getOrDefault(false)
            _state.update { it.copy(checkingServer = false, serverOnline = online) }
        }
    }

    /** Войти в режим редактирования: поле заполняется текущим адресом. */
    fun startEdit() {
        _state.update { it.copy(editing = true, addressInput = it.baseUrl) }
    }

    fun updateAddress(value: String) {
        _state.update { it.copy(addressInput = value) }
    }

    /** Отменить редактирование: вернуть сохранённый адрес. */
    fun cancelEdit() {
        _state.update { it.copy(editing = false, addressInput = it.baseUrl) }
    }

    /**
     * Сохранить адрес. [onSave] персистит нормализованное значение
     * (например, `ServerConfig.save`), после чего связь проверяется заново.
     */
    fun save(onSave: (String) -> Unit) {
        val normalized = normalize(_state.value.addressInput)
        onSave(normalized)
        _state.update { it.copy(baseUrl = normalized, addressInput = normalized, editing = false) }
        checkServer()
    }

    private fun normalize(url: String): String =
        url.trim().trimEnd('/').ifBlank { ServerConfig.DEFAULT_BASE_URL }
}
