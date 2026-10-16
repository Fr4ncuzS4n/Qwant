package com.example.qwant.screens

import com.example.qwant.data.LoginResponse
import com.example.qwant.data.QwantApi
import com.example.qwant.data.Role
import com.example.qwant.data.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты состояния экрана входа: успех, неверные данные, сеть,
 * валидация пустых полей и проверка связи с сервером.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun driverResponse(): LoginResponse = LoginResponse(
        token = "token-1",
        user = UserAccount(
            id = 1,
            login = "ivanov",
            name = "Иванов А.",
            role = Role.DRIVER,
            driverId = 1,
            driverName = "Иванов А.",
            equipmentName = "МРГ-1",
            equipmentType = "МРГ",
        ),
    )

    @Test
    fun `successful login exposes session and role`() = runTest {
        val viewModel = AuthViewModel(loginCall = { _, _ -> driverResponse() }, healthCheck = { true })

        viewModel.updateLogin("ivanov")
        viewModel.updatePassword("1234")
        assertTrue(viewModel.state.value.canSubmit)

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("token-1", state.loggedIn?.token)
        assertEquals(Role.DRIVER, state.loggedIn?.user?.role)
        assertEquals(1, state.loggedIn?.user?.driverId)
        assertNull(state.error)
        assertFalse(state.submitting)
    }

    @Test
    fun `invalid credentials are reported`() = runTest {
        val viewModel = AuthViewModel(
            loginCall = { _, _ -> throw QwantApi.UnauthorizedException("invalid credentials") },
            healthCheck = { true },
        )

        viewModel.updateLogin("ivanov")
        viewModel.updatePassword("wrong")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthError.INVALID, viewModel.state.value.error)
        assertNull(viewModel.state.value.loggedIn)
    }

    @Test
    fun `network failure is reported`() = runTest {
        val viewModel = AuthViewModel(
            loginCall = { _, _ -> throw java.io.IOException("connection refused") },
            healthCheck = { true },
        )

        viewModel.updateLogin("ivanov")
        viewModel.updatePassword("1234")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthError.NETWORK, viewModel.state.value.error)
    }

    @Test
    fun `empty fields are rejected without calling the server`() = runTest {
        var called = false
        val viewModel = AuthViewModel(
            loginCall = { _, _ -> called = true; driverResponse() },
            healthCheck = { true },
        )

        viewModel.updateLogin("   ")
        viewModel.updatePassword("")
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthError.REQUIRED, viewModel.state.value.error)
        assertFalse(called)
    }

    @Test
    fun `password visibility toggles`() {
        val viewModel = AuthViewModel(loginCall = { _, _ -> driverResponse() }, healthCheck = { true })
        assertFalse(viewModel.state.value.passwordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.state.value.passwordVisible)
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.state.value.passwordVisible)
    }

    @Test
    fun `server check reflects availability`() = runTest {
        val online = AuthViewModel(loginCall = { _, _ -> driverResponse() }, healthCheck = { true })
        advanceUntilIdle()
        assertTrue(online.state.value.serverOnline)
        assertFalse(online.state.value.checkingServer)

        val offline = AuthViewModel(loginCall = { _, _ -> driverResponse() }, healthCheck = { false })
        advanceUntilIdle()
        assertFalse(offline.state.value.serverOnline)
        assertFalse(offline.state.value.checkingServer)
    }

    @Test
    fun `editing a field clears the previous error`() = runTest {
        val viewModel = AuthViewModel(
            loginCall = { _, _ -> throw QwantApi.UnauthorizedException("x") },
            healthCheck = { true },
        )
        viewModel.updateLogin("ivanov")
        viewModel.updatePassword("wrong")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(AuthError.INVALID, viewModel.state.value.error)

        viewModel.updatePassword("1234")
        assertNull(viewModel.state.value.error)
    }
}
