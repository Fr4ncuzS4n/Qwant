package com.example.qwant.server

object EquipmentType {
    const val MRG = "MRG" // машина раздачи глинозёма
    const val MPK = "MPK" // машина пробойки корки
}

object OrderType {
    const val OBRABOTKA = "OBRABOTKA" // обработка электролизёра
    const val PODSYPKA = "PODSYPKA"   // подсыпка (МРГ) — со «светофором» срочности
    const val REMONT = "REMONT"       // заявка на ремонт из кабины
}

object OrderStatus {
    const val NEW = "NEW"             // новая, ожидает водителя
    const val ACCEPTED = "ACCEPTED"   // «Взять в работу»
    const val DONE = "DONE"           // «Выполнено»
    const val CANCELLED = "CANCELLED"
}

object ZoneKind {
    const val DANGER = "DANGER"       // опасная зона
    const val DEAD_END = "DEAD_END"   // тупик
}

object Urgency {
    const val GREEN = "GREEN"   // сразу после принятия
    const val ORANGE = "ORANGE" // через 10 минут
    const val RED = "RED"       // через 15 минут
}

object Role {
    const val DRIVER = "DRIVER"         // водитель: взять в работу / выполнить
    const val DISPATCHER = "DISPATCHER" // диспетчер: постановка заявок и контроль
}

/** Учётная запись пользователя (для ответа логина — без секретов). */
data class UserAccount(
    val id: Int,
    val login: String,
    val name: String,
    val role: String,
    val driverId: Int? = null,
    val driverName: String? = null,
    val equipmentName: String? = null,
    val equipmentType: String? = null,
)

data class LoginRequest(val login: String? = null, val password: String? = null)

data class LoginResponse(val token: String, val user: UserAccount)

data class Equipment(
    val id: Int,
    val name: String,
    val type: String,
    val model: String,
    val motohours: Double,
    val status: String,
)

data class Driver(
    val id: Int,
    val name: String,
    val equipmentId: Int?,
    val equipmentName: String?,
    val equipmentType: String?,
)

data class Electrolyzer(
    val id: Int,
    val number: Int,
    val building: String,
    val row: Int,
    val position: Int,
    val half: String,
    val series: String,
    val status: String,
)

data class Zone(
    val id: Int,
    val kind: String,
    val label: String,
    val building: String,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
)

data class Gate(
    val id: Int,
    val name: String,
    val building: String,
    val x: Double,
    val y: Double,
    val isOpen: Boolean,
)

data class DeviationTag(
    val id: Int,
    val label: String,
)

data class Order(
    val id: Int,
    val type: String,
    val status: String,
    val description: String,
    val electrolyzerId: Int?,
    val electrolyzerNumber: Int?,
    val building: String?,
    val row: Int?,
    val position: Int?,
    val half: String?,
    val driverId: Int?,
    val driverName: String?,
    val requestedBy: String?,
    val deviationTagId: Int?,
    val deviationTagLabel: String?,
    val comment: String?,
    val isRepair: Boolean,
    val motohoursDelta: Double?,
    val createdAt: String,
    val createdAtMillis: Long,
    val acceptedAt: String?,
    val acceptedAtMillis: Long?,
    val completedAt: String?,
    val elapsedMinutes: Long?,
    val urgency: String?,
)

/**
 * Корпус (здание) цеха: своя раскладка электролизёров и свой единственный вход
 * (слева или справа). `isDefault` — корпус, который клиент открывает по умолчанию.
 */
data class Building(
    val id: String,
    val name: String,
    val rows: Int,
    val perRow: Int,
    val halfSize: Int,
    val electrolyzerCount: Int,
    val isDefault: Boolean,
)

data class Scheme(
    val building: String,
    val rows: Int,
    val perRow: Int,
    val halfSize: Int,
    val electrolyzerCount: Int,
    val electrolyzers: List<Electrolyzer>,
    val zones: List<Zone>,
    val gates: List<Gate>,
)

data class StatsRow(
    val equipment: Equipment,
    val completedOrders: Int,
    val avgExecutionMinutes: Double?,
)

data class Stats(
    val totalMotohours: Double,
    val byType: Map<String, Double>,
    val rows: List<StatsRow>,
    val ordersTotal: Int,
    val ordersNew: Int,
    val ordersAccepted: Int,
    val ordersDone: Int,
)
