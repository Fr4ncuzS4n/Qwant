package com.example.qwant

import com.example.qwant.data.LoginResponse
import com.example.qwant.data.Role
import com.example.qwant.data.UserAccount

/** Общие тестовые данные для инструментальных тестов. */
object TestData {

    fun driverLogin(token: String = "token-driver"): LoginResponse = LoginResponse(
        token = token,
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

    fun dispatcherLogin(token: String = "token-dispatcher"): LoginResponse = LoginResponse(
        token = token,
        user = UserAccount(
            id = 2,
            login = "dispatcher",
            name = "Диспетчер смены",
            role = Role.DISPATCHER,
        ),
    )
}
