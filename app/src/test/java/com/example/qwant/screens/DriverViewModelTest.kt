package com.example.qwant.screens

import com.example.qwant.data.DeviationTag
import com.example.qwant.data.Driver
import com.example.qwant.data.Equipment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты кабинета водителя: загрузка техники исполнителя (карточка «Машина»)
 * и тегов отклонений.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DriverViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val drivers = listOf(
        Driver(id = 1, name = "Иванов А.", equipmentId = 1, equipmentName = "МРГ-1", equipmentType = "MRG"),
        Driver(id = 2, name = "Петров С.", equipmentId = 3, equipmentName = "МПК-1", equipmentType = "MPK"),
    )

    private val equipment = listOf(
        Equipment(id = 1, name = "МРГ-1", type = "MRG", model = "С8БМ", motohours = 237.5),
        Equipment(id = 3, name = "МПК-1", type = "MPK", model = "С3", motohours = 100.0),
    )

    private fun viewModel(driverId: Int) = DriverViewModel(
        driverId = driverId,
        tagsCall = { emptyList<DeviationTag>() },
        driversCall = { drivers },
        equipmentCall = { equipment },
    )

    @Test
    fun `loads the equipment of the driver`() = runTest {
        val vm = viewModel(driverId = 1)
        advanceUntilIdle()

        val eq = vm.state.value.equipment
        assertEquals("МРГ-1", eq?.name)
        assertEquals("С8БМ", eq?.model)
        assertEquals(237.5, eq?.motohours ?: 0.0, 0.001)
    }

    @Test
    fun `driver without assigned equipment has no machine`() = runTest {
        val vm = viewModel(driverId = 99)
        advanceUntilIdle()

        assertNull(vm.state.value.equipment)
    }
}
