package com.example.qwant.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Фирменная палитра предприятия:
 * «графит / сталь / алюминий + расплав электролита».
 *
 * Предприятие — электролизное производство алюминия: тёмный графит корпусов,
 * серебристо-серый алюминий и оранжево-медный жар расплава (~960 °C) в качестве
 * акцента. Сигнальные цвета «светофора» срочности — отдельные, их не смешивать
 * с фирменными.
 */

// --- Светлая тема: алюминий и графит ---

/** Основной акцент — «расплав» (кнопки, активные элементы) */
val FurnaceOrange = Color(0xFFB34A1E)
val FurnaceOrangeLight = Color(0xFFE07A43)

/** Вторичный — тёмная сталь */
val SteelBlue = Color(0xFF3E4C59)
val SteelBlueLight = Color(0xFF8C9BA8)

/** Поверхности светлой темы — светлый алюминий */
val AluminumSurface = Color(0xFFF5F6F7)
val AluminumSurfaceVariant = Color(0xFFE3E6E9)
val AluminumOutline = Color(0xFFBFC6CC)

/** Тёмный графит для текста на светлом */
val Graphite = Color(0xFF1B1F23)
val GraphiteSoft = Color(0xFF444C54)

// --- Тёмная тема: графит и раскалённый металл ---

/** Поверхности тёмной темы — графит цеха */
val GraphiteBackground = Color(0xFF121417)
val GraphiteSurface = Color(0xFF1C2024)
val GraphiteSurfaceVariant = Color(0xFF2A3036)
val GraphiteOutline = Color(0xFF4A545C)

/** Акцент тёмной темы — расплав (ярче, для контраста) */
val MoltenOrange = Color(0xFFFF8A50)
val MoltenOrangeContainer = Color(0xFF7A2E0E)

val SteelBlueDark = Color(0xFFAEBBC7)

// --- Сигнальные цвета (светофор срочности, статусы, схема корпуса) ---

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

/** Проезды техники на схеме корпуса: между рядами и вдоль стены. */
val LaneDrive = Color(0xFF607D8B)

/** Опасная зона на схеме */
val ZoneDanger = Color(0xFFD32F2F)

/** Тупик на схеме */
val ZoneDeadEnd = Color(0xFF6D4C41)
