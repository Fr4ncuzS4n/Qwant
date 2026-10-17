package com.example.qwant.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.hypot
import kotlin.math.max

/**
 * Минимальный размер зоны касания элемента схемы, dp.
 *
 * Ячейка электролизёра на плане корпуса мелкая (на телефоне — единицы dp по
 * высоте), поэтому попадание пальцем по ней считается **не по контуру**, а по
 * увеличенной зоне касания: палец «прилипает» к ближайшему электролизёру
 * (или воротам) — см. [nearestTouch].
 */
const val SCHEME_MIN_TOUCH_DP = 48f

/** Расстояние от точки [tap] до ближайшей точки прямоугольника [rect] (0 — точка внутри). */
private fun distanceToRect(rect: Rect, tap: Offset): Float {
    val dx = max(max(rect.left - tap.x, tap.x - rect.right), 0f)
    val dy = max(max(rect.top - tap.y, tap.y - rect.bottom), 0f)
    return hypot(dx, dy)
}

/** Расстояние от точки [tap] до центра прямоугольника [rect]. */
private fun distanceToCenter(rect: Rect, tap: Offset): Float =
    hypot(rect.center.x - tap.x, rect.center.y - tap.y)

/**
 * Попадание пальцем по схеме корпуса: какая зона выбрана тапом в точке [tap].
 *
 * Схема мелкая, поэтому одного «попадания в контур» мало:
 *  - тап внутри контура зоны — выбирается эта зона (дальше решает расстояние);
 *  - иначе контур расширяется на [minTouch] / 2 в каждую сторону — получается
 *    зона касания размером не меньше [minTouch] даже у самой мелкой ячейки;
 *  - из зон, чья увеличенная зона накрыла тап, выбирается **ближайшая**;
 *  - при равном расстоянии выигрывает зона с ближайшим центром;
 *  - если тап не накрыл ни одной зоны — `null` (промах).
 *
 * Функция не зависит от Compose: покрыта unit-тестами `SchemeTapTest`.
 * Универсальна по типу значения [T]: и электролизёр, и ворота.
 */
fun <T> nearestTouch(
    zones: List<Pair<Rect, T>>,
    tap: Offset,
    minTouch: Float,
): T? {
    val pad = minTouch / 2f
    var best: T? = null
    var bestDistance = Float.MAX_VALUE
    var bestCenter = Float.MAX_VALUE

    zones.forEach { (rect, value) ->
        val distance = distanceToRect(rect, tap)
        if (distance > pad) return@forEach
        val center = distanceToCenter(rect, tap)
        if (distance < bestDistance || (distance == bestDistance && center < bestCenter)) {
            best = value
            bestDistance = distance
            bestCenter = center
        }
    }
    return best
}
