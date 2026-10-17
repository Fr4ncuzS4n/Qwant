package com.example.qwant.screens

import com.example.qwant.data.Role

/**
 * Маршруты навигации и правила доступа по ролям.
 *
 * Роли разграничивают экраны: кабинет и постановка заявок доступны только
 * диспетчеру, кабинет водителя — только водителю. Общие экраны (схема корпуса,
 * ворота, статистика) доступны обеим ролям.
 */
object Routes {

    /** Экран входа (логин/пароль). */
    const val AUTH = "auth"

    /** Служебный экран «Для разработчика» (настройка адреса сервера). */
    const val DEVELOPER = "developer"

    const val DRIVER = "driver"
    const val DISPATCHER = "dispatcher"
    const val CREATE_ORDER = "create_order"
    const val ORDER = "order/{orderId}"
    const val SCHEME = "scheme"
    const val GATES = "gates"
    const val STATS = "stats"

    fun order(id: Int) = "order/$id"

    /** Экраны, доступные только диспетчеру. */
    private val dispatcherOnly = setOf(DISPATCHER, CREATE_ORDER)

    /** Экраны, доступные только водителю. */
    private val driverOnly = setOf(DRIVER)

    /** Домашний экран (кабинет) для роли; при неизвестной роли — кабинет водителя. */
    fun homeFor(role: String?): String = when (role) {
        Role.DISPATCHER -> DISPATCHER
        else -> DRIVER
    }

    /** Стартовый маршрут: без сессии — вход, с сессией — кабинет роли. */
    fun startFor(role: String?): String = if (role == null) AUTH else homeFor(role)

    /**
     * Разрешён ли маршрут роли. Без сессии ([role] == null) доступны только вход
     * и служебный экран «Для разработчика» (настройка сервера до входа);
     * общие экраны требуют входа.
     */
    fun isAllowed(role: String?, route: String): Boolean {
        val target = route.substringBefore('/')
        if (target == AUTH || target == DEVELOPER) return true
        if (role == null) return false
        if (target in dispatcherOnly) return role == Role.DISPATCHER
        if (target in driverOnly) return role == Role.DRIVER
        return true
    }
}
