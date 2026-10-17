package com.example.qwant.screens

import com.example.qwant.data.Building
import com.example.qwant.data.Order
import com.example.qwant.data.OrderStatus
import com.example.qwant.data.OrderType
import com.example.qwant.data.QwantApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Юнит-тесты создания заявки диспетчером: выбор корпуса (куда отправить
 * водителя), переключение схемы корпуса и постановка заявки.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val buildings = listOf(
        Building(id = "4", name = "Корпус 4", electrolyzerCount = 150, isDefault = true),
        Building(id = "3", name = "Корпус 3", electrolyzerCount = 150),
        Building(id = "2", name = "Корпус 2", electrolyzerCount = 150),
    )

    private fun schemeOf(building: String) = Scheme(
        building = building,
        rows = 2,
        perRow = 75,
        halfSize = 37,
        electrolyzerCount = 150,
    )

    private fun viewModel(
        schemeCall: suspend (String) -> Scheme = { schemeOf(it) },
        createCall: suspend (QwantApi.CreateOrderRequest) -> Order = { throw IllegalStateException("unused") },
    ) = CreateOrderViewModel(
        requesterName = "Диспетчер смены",
        buildingsCall = { buildings },
        schemeCall = schemeCall,
        createCall = createCall,
    )

    @Test
    fun `load selects the default building and loads its scheme`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("4", state.buildingId)
        assertEquals("4", state.scheme?.building)
        assertEquals(3, state.buildings.size)
        assertTrue(!state.loading)
    }

    @Test
    fun `selectBuilding loads the scheme of the chosen building and resets the electrolyzer`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectElectrolyzer(42)
        vm.selectBuilding("2")
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("2", state.buildingId)
        assertEquals("2", state.scheme?.building)
        assertNull(state.electrolyzerId)
    }

    @Test
    fun `submit requires an electrolyzer`() = runTest {
        var submitted = false
        val vm = viewModel(createCall = { submitted = true; error("must not be called") })
        advanceUntilIdle()

        var result: Result<Order>? = null
        vm.submit { result = it }
        advanceUntilIdle()

        assertTrue(result!!.isFailure)
        assertTrue(!submitted)
    }

    @Test
    fun `submit sends the chosen type, electrolyzer and description`() = runTest {
        var request: QwantApi.CreateOrderRequest? = null
        val vm = viewModel(createCall = { req ->
            request = req
            Order(id = 7, type = req.type, status = OrderStatus.NEW, electrolyzerId = req.electrolyzerId)
        })
        advanceUntilIdle()

        vm.selectType(OrderType.PODSYPKA)
        vm.selectElectrolyzer(125)
        vm.submit { }
        advanceUntilIdle()

        val req = request!!
        assertEquals(OrderType.PODSYPKA, req.type)
        assertEquals(125, req.electrolyzerId)
        assertEquals("Подсыпка глинозёма", req.description)
    }
}
