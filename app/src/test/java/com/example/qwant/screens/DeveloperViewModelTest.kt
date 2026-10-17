package com.example.qwant.screens

import com.example.qwant.data.ServerConfig
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты экрана «Для разработчика»: проверка связи, режим редактирования
 * адреса сервера, нормализация и сохранение.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `server check reflects availability`() = runTest {
        val online = DeveloperViewModel(healthCheck = { true })
        advanceUntilIdle()
        assertTrue(online.state.value.serverOnline)
        assertFalse(online.state.value.checkingServer)

        val offline = DeveloperViewModel(healthCheck = { false })
        advanceUntilIdle()
        assertFalse(offline.state.value.serverOnline)
        assertFalse(offline.state.value.checkingServer)
    }

    @Test
    fun `start edit fills the input with the saved address`() = runTest {
        val viewModel = DeveloperViewModel(healthCheck = { true })
        advanceUntilIdle()

        viewModel.startEdit()

        assertTrue(viewModel.state.value.editing)
        assertEquals(ServerConfig.baseUrl, viewModel.state.value.addressInput)
    }

    @Test
    fun `cancel edit restores the saved address`() = runTest {
        val viewModel = DeveloperViewModel(healthCheck = { true })
        advanceUntilIdle()

        viewModel.startEdit()
        viewModel.updateAddress("http://192.168.1.10:8080")
        viewModel.cancelEdit()

        assertFalse(viewModel.state.value.editing)
        assertEquals(ServerConfig.baseUrl, viewModel.state.value.addressInput)
    }

    @Test
    fun `save normalizes the address and exits edit mode`() = runTest {
        val viewModel = DeveloperViewModel(healthCheck = { true })
        advanceUntilIdle()

        var saved: String? = null
        viewModel.startEdit()
        viewModel.updateAddress("  http://192.168.1.10:8080/ ")
        viewModel.save { saved = it }

        assertEquals("http://192.168.1.10:8080", saved)
        assertEquals("http://192.168.1.10:8080", viewModel.state.value.baseUrl)
        assertFalse(viewModel.state.value.editing)
    }

    @Test
    fun `blank address falls back to the default`() = runTest {
        val viewModel = DeveloperViewModel(healthCheck = { true })
        advanceUntilIdle()

        var saved: String? = null
        viewModel.startEdit()
        viewModel.updateAddress("   ")
        viewModel.save { saved = it }

        assertEquals(ServerConfig.DEFAULT_BASE_URL, saved)
    }
}
