package com.example.qwant.ui

import com.example.qwant.data.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale("ru", "RU"))
private val dateTimeFormat = SimpleDateFormat("dd.MM HH:mm", Locale("ru", "RU"))

/** Наработка в моточасах: 237.5 -> «237,5». */
fun Double.asMotohours(): String = String.format(Locale("ru", "RU"), "%.1f", this)

/** Время создания/принятия заявки в формате ЧЧ:мм. */
fun Long.asTime(): String = timeFormat.format(Date(this))

/** Дата и время, например «16.10 14:35». */
fun Long.asDateTime(): String = dateTimeFormat.format(Date(this))

/** Длительность в минутах: 12 -> «12 мин», 95 -> «1 ч 35 мин». */
fun Int.asDuration(): String =
    if (this < 60) "$this мин" else "${this / 60} ч ${this % 60} мин"

/** Короткое описание места на схеме: «Корпус 4 · Электролизер №125». */
fun Order.place(): String = buildString {
    building?.let { append("Корпус $it") }
    electrolyzerNumber?.let {
        if (isNotEmpty()) append(" · ")
        append("Электролизер №$it")
    }
}
