package com.example.qwant.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- Производственная палитра (светофор срочности, статусы, схема корпуса) ---

/** 🟢 заявка в норме */
val SignalGreen = Color(0xFF2E7D32)

/** 🟠 нарастающее отклонение (10 минут) */
val SignalOrange = Color(0xFFEF6C00)

/** 🔴 просрочено (15 минут) */
val SignalRed = Color(0xFFC62828)

/** Статус NEW */
val StatusNew = Color(0xFF1565C0)

/** Статус ACCEPTED */
val StatusAccepted = Color(0xFF00838F)

/** Статус DONE */
val StatusDone = Color(0xFF2E7D32)

/** Электролизёр в обслуживании */
val ElectrolyzerService = Color(0xFFF9A825)

/** Опасная зона на схеме */
val ZoneDanger = Color(0xFFD32F2F)

/** Тупик на схеме */
val ZoneDeadEnd = Color(0xFF6D4C41)