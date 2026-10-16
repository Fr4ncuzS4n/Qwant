package com.example.qwant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.qwant.data.ServerConfig
import com.example.qwant.data.SessionStore
import com.example.qwant.screens.CreateOrderScreen
import com.example.qwant.screens.DispatcherScreen
import com.example.qwant.screens.DriverScreen
import com.example.qwant.screens.DriverSelectScreen
import com.example.qwant.screens.GatesScreen
import com.example.qwant.screens.MainScreen
import com.example.qwant.screens.OrderScreen
import com.example.qwant.screens.SchemeScreen
import com.example.qwant.screens.StatisticScreen
import com.example.qwant.ui.theme.QwantTheme

/** Маршруты навигации приложения. */
object Routes {
    const val MAIN = "main"
    const val DRIVER_SELECT = "driver_select"
    const val DRIVER = "driver"
    const val DISPATCHER = "dispatcher"
    const val CREATE_ORDER = "create_order"
    const val ORDER = "order/{orderId}"
    const val SCHEME = "scheme"
    const val GATES = "gates"
    const val STATS = "stats"

    fun order(id: Int) = "order/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Адрес сервера читается до первого запроса API.
        ServerConfig.init(this)
        setContent {
            QwantTheme {
                QwantApp()
            }
        }
    }
}

/** Клиент-серверное приложение: два личных кабинета — водителя и диспетчера. */
@Composable
private fun QwantApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.MAIN) {

        composable(Routes.MAIN) {
            MainScreen(
                onOpenDriver = {
                    val destination = if (SessionStore.isDriverSelected(context)) {
                        Routes.DRIVER
                    } else {
                        Routes.DRIVER_SELECT
                    }
                    navController.navigate(destination)
                },
                onOpenDispatcher = { navController.navigate(Routes.DISPATCHER) },
            )
        }

        composable(Routes.DRIVER_SELECT) {
            DriverSelectScreen(
                onDriverSelected = { driver ->
                    SessionStore.saveDriver(
                        context = context,
                        id = driver.id,
                        name = driver.name,
                        equipmentName = driver.equipmentName,
                    )
                    navController.navigate(Routes.DRIVER) {
                        popUpTo(Routes.MAIN)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DRIVER) {
            val driverId = SessionStore.driverId(context)
            if (driverId <= 0) {
                // Сессия сброшена — возвращаемся к выбору водителя.
                DriverSelectScreen(
                    onDriverSelected = { driver ->
                        SessionStore.saveDriver(
                            context = context,
                            id = driver.id,
                            name = driver.name,
                            equipmentName = driver.equipmentName,
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            } else {
                DriverScreen(
                    driverId = driverId,
                    driverName = SessionStore.driverName(context),
                    equipmentName = SessionStore.equipmentName(context),
                    onOpenOrder = { id -> navController.navigate(Routes.order(id)) },
                    onOpenScheme = { navController.navigate(Routes.SCHEME) },
                    onOpenGates = { navController.navigate(Routes.GATES) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onChangeDriver = {
                        SessionStore.clearDriver(context)
                        navController.navigate(Routes.DRIVER_SELECT) {
                            popUpTo(Routes.MAIN)
                        }
                    },
                )
            }
        }

        composable(Routes.DISPATCHER) {
            DispatcherScreen(
                onCreateOrder = { navController.navigate(Routes.CREATE_ORDER) },
                onOpenOrder = { id -> navController.navigate(Routes.order(id)) },
                onOpenScheme = { navController.navigate(Routes.SCHEME) },
                onOpenGates = { navController.navigate(Routes.GATES) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CREATE_ORDER) {
            CreateOrderScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.ORDER,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType }),
        ) { entry ->
            OrderScreen(
                orderId = entry.arguments?.getInt("orderId") ?: 0,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SCHEME) {
            SchemeScreen(
                onBack = { navController.popBackStack() },
                onOpenGates = { navController.navigate(Routes.GATES) },
            )
        }

        composable(Routes.GATES) {
            GatesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STATS) {
            StatisticScreen(onBack = { navController.popBackStack() })
        }
    }
}
