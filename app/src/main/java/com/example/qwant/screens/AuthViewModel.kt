package com.example.qwant.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qwant.data.LoginResponse
import com.example.qwant.data.QwantApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Причина, по которой вход не удался (экран показывает русский текст). */
enum class AuthError {
    REQUIRED, // не заполнены логин/пароль
    INVALID, // неверный логин или пароль
    NETWORK, // сервер недоступен
}

data class AuthUiState(
    val login: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false,
    val error: AuthError? = null,
    /** Заполняется при успешном входе — экран сохраняет сессию и переходит дальше. */
    val loggedIn: LoginResponse? = null,
    val checkingServer: Boolean = true,
    val serverOnline: Boolean = false,
) {
    val canSubmit: Boolean
        get() = login.isNotBlank() && password.isNotBlank() && !submitting
}

/**
 * Состояние экрана входа.
 *
 * Зависимости ([loginCall], [healthCheck]) передаются в конструктор — это
 * позволяет тестировать ViewModel без сервера и без Android-контекста.
 */
class AuthViewModel(
    private val loginCall: suspend (String, String) -> LoginResponse = { login, password ->
        QwantApi.login(login, password)
    },
    private val healthCheck: suspend () -> Boolean = { QwantApi.health().status == "ok" },
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        checkServer()
    }

    fun updateLogin(value: String) {
        _state.update { it.copy(login = value, error = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /** Проверка доступности сервера (индикатор на экране входа). */
    fun checkServer() {
        _state.update { it.copy(checkingServer = true) }
        viewModelScope.launch {
            val online = runCatching { healthCheck() }.getOrDefault(false)
            _state.update { it.copy(checkingServer = false, serverOnline = online) }
        }
    }

    /** Вход: логин/пароль -> сессия. Успех кладёт [AuthUiState.loggedIn]. */
    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val login = current.login.trim()
        val password = current.password
        if (login.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(error = AuthError.REQUIRED) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            runCatching { loginCall(login, password) }
                .onSuccess { response ->
                    _state.update { it.copy(submitting = false, loggedIn = response, error = null) }
                }
                .onFailure { throwable ->
                    val error = if (throwable is QwantApi.UnauthorizedException) {
                        AuthError.INVALID
                    } else {
                        AuthError.NETWORK
                    }
                    _state.update { it.copy(submitting = false, error = error) }
                }
        }
    }
}
