package com.example.qwant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import com.example.qwant.data.QwantApi
import com.example.qwant.data.ServerConfig
import com.example.qwant.data.SessionStore
import com.example.qwant.data.UserSession
import com.example.qwant.screens.AuthScreen
import com.example.qwant.screens.CreateOrderScreen
import com.example.qwant.screens.DeveloperScreen
import com.example.qwant.screens.DispatcherScreen
import com.example.qwant.screens.DriverScreen
import com.example.qwant.screens.GatesScreen
import com.example.qwant.screens.OrderScreen
import com.example.qwant.screens.Routes
import com.example.qwant.screens.SchemeScreen
import com.example.qwant.screens.StatisticScreen
import com.example.qwant.ui.theme.QwantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Адрес сервера и токен сессии читаются до первого запроса API.
        ServerConfig.init(this)
        QwantApi.authToken = SessionStore.session(this)?.token
        setContent {
            QwantTheme {
                QwantApp()
            }
        }
    }
}

/**
 * Клиент-серверное приложение с разграничением ролей:
 * диспетчер и водитель входят под своим логином и видят только свои экраны.
 */
@Composable
private fun QwantApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Сессия — источник истины для навигации по ролям. Храним её в State и читаем
    // **внутри** экранов: граф навигации строится один раз, а доступ по роли
    // всегда проверяется по актуальной сессии. Если передать значение сессии,
    // то сразу после входа проверка сработает по устаревшему (пустому) значению
    // и пользователя вернёт на экран входа — приходилось входить дважды.
    val session = remember { mutableStateOf(SessionStore.session(context)) }

    fun logout() {
        SessionStore.clear(context)
        QwantApi.authToken = null
        session.value = null
        navController.navigate(Routes.AUTH) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Граф создаётся один раз; стартовый экран — по сессии на момент запуска.
    val graph = remember(navController) {
        navController.createGraph(
            startDestination = Routes.startFor(session.value?.role),
        ) {
            composable(Routes.AUTH) {
                AuthScreen(
                    onLoggedIn = { role ->
                        // Токен и данные учётной записи экран входа уже сохранил.
                        session.value = SessionStore.session(context)
                        navController.navigate(Routes.homeFor(role)) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenDeveloper = { navController.navigate(Routes.DEVELOPER) },
                )
            }

            // Служебный экран настройки сервера (доступен без входа).
            composable(Routes.DEVELOPER) {
                DeveloperScreen(onBack = { navController.popBackStack() })
            }

            // --- кабинет водителя (только DRIVER) ---
            guardedComposable(Routes.DRIVER, session, navController) { current ->
                val driverId = current?.driverId
                if (driverId == null) {
                    // Учётная запись без привязанной техники — возвращаемся ко входу.
                    LaunchedEffect(Unit) { logout() }
                } else {
                    DriverScreen(
                        driverId = driverId,
                        driverName = current.driverName ?: current.name,
                        equipmentName = current.equipmentName,
                        driverLogin = current.login,
                        equipmentType = current.equipmentType,
                        onOpenOrder = { id -> navController.navigate(Routes.order(id)) },
                        onOpenScheme = { navController.navigate(Routes.SCHEME) },
                        onOpenGates = { navController.navigate(Routes.GATES) },
                        onOpenStats = { navController.navigate(Routes.STATS) },
                        onLogout = { logout() },
                    )
                }
            }

            // --- кабинет диспетчера (только DISPATCHER) ---
            guardedComposable(Routes.DISPATCHER, session, navController) { current ->
                DispatcherScreen(
                    dispatcherName = current?.name.orEmpty(),
                    onCreateOrder = { navController.navigate(Routes.CREATE_ORDER) },
                    onOpenOrder = { id -> navController.navigate(Routes.order(id)) },
                    onOpenScheme = { navController.navigate(Routes.SCHEME) },
                    onOpenGates = { navController.navigate(Routes.GATES) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onLogout = { logout() },
                )
            }

            guardedComposable(Routes.CREATE_ORDER, session, navController) { current ->
                CreateOrderScreen(
                    requestedBy = current?.name.orEmpty(),
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

    NavHost(navController = navController, graph = graph)
}

/**
 * Экранированный маршрут: если роль не имеет доступа, пользователь
 * перенаправляется на свой домашний экран (или на вход).
 *
 * Сессия передаётся как [State] и читается **при отрисовке экрана** — так проверка
 * всегда видит актуальную роль, а не значение на момент построения графа.
 */
private fun NavGraphBuilder.guardedComposable(
    route: String,
    session: State<UserSession?>,
    navController: NavHostController,
    content: @Composable (UserSession?) -> Unit,
) {
    composable(route) {
        val current = session.value
        if (Routes.isAllowed(current?.role, route)) {
            content(current)
        } else {
            LaunchedEffect(current) {
                navController.navigate(Routes.startFor(current?.role)) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}
