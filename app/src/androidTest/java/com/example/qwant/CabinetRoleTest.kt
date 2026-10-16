package com.example.qwant

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.qwant.screens.DispatcherScreen
import com.example.qwant.screens.DriverScreen
import com.example.qwant.ui.theme.QwantTheme
import org.junit.Rule
import org.junit.Test

/**
 * Тесты разграничения экранов по ролям: у водителя нет экранов диспетчера,
 * у диспетчера — экранов водителя. Проверяется по характерным элементам UI.
 */
class CabinetRoleTest {

    @get:Rule
    val rule = createComposeRule()

    private val dispatcherCreate = "Создать заявку"
    private val driverCabinet = "Кабинет водителя"
    private val dispatcherCabinet = "Кабинет диспетчера"
    private val logout = "Выйти"

    @Test
    fun driverCabinetHasNoDispatcherScreens() {
        rule.setContent {
            QwantTheme {
                // ViewModel создаётся фабрикой по умолчанию: DriverViewModel(driverId).
                DriverScreen(
                    driverId = 1,
                    driverName = "Иванов А.",
                    equipmentName = "МРГ-1",
                    onOpenOrder = {},
                    onOpenScheme = {},
                    onOpenGates = {},
                    onOpenStats = {},
                    onLogout = {},
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText(driverCabinet).assertExists()
        rule.onNodeWithText(logout).assertExists()
        rule.onNodeWithText(dispatcherCreate).assertDoesNotExist()
        rule.onNodeWithText(dispatcherCabinet).assertDoesNotExist()
    }

    @Test
    fun dispatcherCabinetHasNoDriverScreen() {
        rule.setContent {
            QwantTheme {
                DispatcherScreen(
                    dispatcherName = "Диспетчер смены",
                    onCreateOrder = {},
                    onOpenOrder = {},
                    onOpenScheme = {},
                    onOpenGates = {},
                    onOpenStats = {},
                    onLogout = {},
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText(dispatcherCabinet).assertExists()
        rule.onNodeWithText(dispatcherCreate, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(logout).assertExists()
        rule.onNodeWithText(driverCabinet).assertDoesNotExist()
    }
}
