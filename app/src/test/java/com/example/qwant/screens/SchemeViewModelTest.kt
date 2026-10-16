package com.example.qwant.screens

import com.example.qwant.data.Building
import com.example.qwant.data.Gate
import com.example.qwant.data.Scheme
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты экрана схемы: загрузка списка корпусов, выбор корпуса (смена
 * схемы и сброс фрагмента/выбора) и дистанционное управление воротами.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SchemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun building(id: String, isDefault: Boolean, count: Int) = Building(
        id = id,
        name = "Корпус $id",
        rows = 5,
        perRow = count / 5,
        halfSize = count / 10,
        electrolyzerCount = count,
        isDefault = isDefault,
    )

    private fun scheme(id: String, count: Int, gates: List<Gate> = emptyList()) = Scheme(
        building = id,
        rows = 5,
        perRow = count / 5,
        halfSize = count / 10,
        electrolyzerCount = count,
        gates = gates,
    )

    @Test
    fun `loads buildings and the default one`() = runTest {
        val viewModel = SchemeViewModel(
            buildingsCall = { listOf(building("4", true, 150), building("3", false, 100)) },
            schemeCall = { id -> scheme(id, if (id == "4") 150 else 100) },
            gateCall = { it },
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals(listOf("4", "3"), state.buildings.map { it.id })
        assertEquals("4", state.buildingId)
        assertEquals("4", state.scheme?.building)
        assertEquals(30, state.scheme?.perRow)
        assertNull(state.error)
    }

    @Test
    fun `selecting a building reloads its scheme and resets the selection`() = runTest {
        val viewModel = SchemeViewModel(
            buildingsCall = { listOf(building("4", true, 150), building("3", false, 100)) },
            schemeCall = { id -> scheme(id, if (id == "4") 150 else 100) },
            gateCall = { it },
        )
        advanceUntilIdle()

        viewModel.selectElectrolyzer(10)
        viewModel.selectBuilding("3")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("3", state.buildingId)
        assertEquals("3", state.scheme?.building)
        assertEquals(20, state.scheme?.perRow)
        assertNull(state.selectedElectrolyzerId)
    }

    @Test
    fun `switching back to a loaded building is served from the cache`() = runTest {
        var schemeCalls = 0
        val viewModel = SchemeViewModel(
            buildingsCall = { listOf(building("4", true, 150), building("3", false, 100)) },
            schemeCall = { id ->
                schemeCalls++
                scheme(id, if (id == "4") 150 else 100)
            },
            gateCall = { it },
        )
        advanceUntilIdle()
        assertEquals(1, schemeCalls) // схема корпуса по умолчанию

        viewModel.selectBuilding("3")
        advanceUntilIdle()
        assertEquals(2, schemeCalls)

        // возврат к уже загруженному корпусу — без обращения к сети
        viewModel.selectBuilding("4")
        advanceUntilIdle()
        assertEquals("4", viewModel.state.value.scheme?.building)
        assertEquals(2, schemeCalls)
    }

    @Test
    fun `reports a network error when buildings fail to load`() = runTest {
        val viewModel = SchemeViewModel(
            buildingsCall = { throw java.io.IOException("connection refused") },
            schemeCall = { scheme(it, 150) },
            gateCall = { it },
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertNotNull(state.error)
        assertNull(state.scheme)
    }

    @Test
    fun `toggle gate updates the gate in the scheme`() = runTest {
        val gate = Gate(id = 1, name = "Вход в корпус 4", x = 0f, isOpen = false)
        val viewModel = SchemeViewModel(
            buildingsCall = { listOf(building("4", true, 150)) },
            schemeCall = { scheme(it, 150, gates = listOf(gate)) },
            gateCall = { current -> current.copy(isOpen = !current.isOpen) },
        )
        advanceUntilIdle()

        viewModel.toggleGate(gate)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.scheme?.gates?.first()?.isOpen == true)
        assertNull(state.busyGateId)
    }
}
