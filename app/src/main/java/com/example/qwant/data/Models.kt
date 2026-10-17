package com.example.qwant.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Доменные модели ответов сервера (`server/`, REST API «/api/…»).
 *
 * Сервер сериализует JSON через Gson и не выводит поля со значением `null`,
 * поэтому у всех необязательных полей есть значение по умолчанию.
 */

// --- Типы и статусы заявок (совпадают с константами сервера) ---

object OrderType {
    const val OBRABOTKA = "OBRABOTKA" // обработка корки
    const val PODSYPKA = "PODSYPKA" // подсыпка глинозёма (МРГ, «светофор»)
    const val REMONT = "REMONT" // заявка на ремонт
}

object OrderStatus {
    const val NEW = "NEW"
    const val ACCEPTED = "ACCEPTED"
    const val DONE = "DONE"
    const val CANCELLED = "CANCELLED"
}

object ZoneKind {
    const val DANGER = "DANGER"
    const val DEAD_END = "DEAD_END"
}

object Urgency {
    const val GREEN = "GREEN"
    const val ORANGE = "ORANGE"
    const val RED = "RED"
}

/** Роли пользователей: кабинеты и права различаются. */
object Role {
    const val DRIVER = "DRIVER"
    const val DISPATCHER = "DISPATCHER"
}

/** Типы техники (`GET /api/equipment`), совпадают с константами сервера. */
object EquipmentType {
    const val MRG = "MRG" // машина раздачи глинозёма
    const val MPK = "MPK" // машина пробойки корки
}

/** Авторизованный пользователь (`POST /api/auth/login`). */
@Serializable
data class UserAccount(
    val id: Int,
    val login: String = "",
    val name: String = "",
    val role: String = Role.DRIVER,
    val driverId: Int? = null,
    val driverName: String? = null,
    val equipmentName: String? = null,
    val equipmentType: String? = null,
)

@Serializable
data class LoginRequest(val login: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: UserAccount)

@Serializable
data class Driver(
    val id: Int,
    val name: String,
    val equipmentId: Int? = null,
    val equipmentName: String? = null,
    val equipmentType: String? = null,
)

@Serializable
data class Order(
    val id: Int,
    val type: String,
    val status: String,
    val description: String? = null,
    val electrolyzerId: Int? = null,
    val electrolyzerNumber: Int? = null,
    val building: String? = null,
    val row: Int? = null,
    val position: Int? = null,
    val half: String? = null,
    val driverId: Int? = null,
    val driverName: String? = null,
    val requestedBy: String? = null,
    val deviationTagId: Int? = null,
    val deviationTagLabel: String? = null,
    val comment: String? = null,
    val isRepair: Boolean = false,
    val motohoursDelta: Double? = null,
    val createdAt: String? = null,
    val createdAtMillis: Long = 0L,
    val acceptedAt: String? = null,
    val acceptedAtMillis: Long? = null,
    val completedAt: String? = null,
    /** Время с момента принятия в работе, минуты (считает сервер). */
    val elapsedMinutes: Int? = null,
    /** Цвет «светофора»: GREEN / ORANGE / RED, только для PODSYPKA в статусе ACCEPTED. */
    val urgency: String? = null,
)

@Serializable
data class Electrolyzer(
    val id: Int,
    val number: Int,
    val building: String,
    val row: Int,
    val position: Int,
    val half: String,
    val series: String? = null,
    val status: String = "OK",
)

@Serializable
data class Zone(
    val id: Int,
    val kind: String,
    val label: String,
    val building: String? = null,
    /** Нормализованные координаты прямоугольника зоны: 0..1. */
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
)

@Serializable
data class Gate(
    val id: Int,
    val name: String,
    val building: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    @SerialName("isOpen") val isOpen: Boolean = false,
)

@Serializable
data class Scheme(
    val building: String,
    val rows: Int,
    val perRow: Int,
    val halfSize: Int,
    val electrolyzerCount: Int,
    val electrolyzers: List<Electrolyzer> = emptyList(),
    val zones: List<Zone> = emptyList(),
    val gates: List<Gate> = emptyList(),
)

/**
 * Корпус цеха (`GET /api/buildings`): у каждого своя раскладка электролизёров
 * и свой единственный вход. `isDefault` — корпус, открываемый по умолчанию.
 */
@Serializable
data class Building(
    val id: String,
    val name: String = "",
    val rows: Int = 0,
    val perRow: Int = 0,
    val halfSize: Int = 0,
    val electrolyzerCount: Int = 0,
    val isDefault: Boolean = false,
)

@Serializable
data class DeviationTag(
    val id: Int,
    val label: String,
)

@Serializable
data class Equipment(
    val id: Int,
    val name: String,
    val type: String,
    val model: String? = null,
    val motohours: Double = 0.0,
    val status: String? = null,
)

@Serializable
data class StatsRow(
    val equipment: Equipment,
    val completedOrders: Int = 0,
    val avgExecutionMinutes: Double? = null,
)

@Serializable
data class Stats(
    val totalMotohours: Double = 0.0,
    val byType: Map<String, Double> = emptyMap(),
    val rows: List<StatsRow> = emptyList(),
    val ordersTotal: Int = 0,
    val ordersNew: Int = 0,
    val ordersAccepted: Int = 0,
    val ordersDone: Int = 0,
)

@Serializable
data class Health(
    val status: String = "",
    val service: String? = null,
    val time: String? = null,
)
