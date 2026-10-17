package com.example.qwant.ui

import com.example.qwant.data.Scheme

/**
 * Формат схемы корпуса. На телефоне и на планшете электролизёры и проезды
 * располагаются по-разному: на планшете видно больше электролизёров в ряду,
 * на телефоне — меньше и крупнее (см. [HallLayout.colsPerView]).
 */
enum class HallFormat { PHONE, TABLET }

/** Сторона корпуса, в которой расположен единственный вход. */
enum class HallSide { LEFT, RIGHT }

/** Прямоугольник в координатах корпуса (условные метры, начало — левый верхний угол). */
data class Box(val x: Float, val y: Float, val w: Float, val h: Float) {
    val right: Float get() = x + w
    val bottom: Float get() = y + h
}

/**
 * Видимое «окно» схемы: какие ряды и позиции электролизёров показаны.
 * Координаты — 1-based, как в API (`row`, `position`).
 */
data class SchemeFragment(
    val rowFrom: Int = 1,
    val rowTo: Int = 1,
    val colFrom: Int = 1,
    val colTo: Int = 1,
) {
    val rows: Int get() = rowTo - rowFrom + 1
    val cols: Int get() = colTo - colFrom + 1
}

/**
 * Геометрия корпуса электролиза.
 *
 * Устройство корпуса (по аналогии с реальным цехом):
 *  - электролизёры стоят **двумя линиями** ([rows]) — по одной с каждой стороны
 *    центральной дороги, — **по отдельности** ([cellBox]): при [BLOCK_SIZE] = 1
 *    группировки нет, поэтому между каждой парой соседей — проезд техники,
 *    а узкий служебный зазор [CELL_GAP] не используется;
 *  - между электролизёрами — **проезд техники** [BLOCK_GAP] (уже самого
 *    электролизёра), которым МРГ/МПК выезжает с линии на центральную дорогу
 *    ([passageBox]);
 *  - по центру корпуса — широкая **центральная дорога** [AISLE_H] ([aisleBox])
 *    для разворота к электролизёрам; по высоте она совпадает с положением входа;
 *  - вдоль боковых и торцевых стен — **дороги** ([sideLanes], [turnLanes]) для
 *    выезда к воротам;
 *  - в корпусе **один вход** ([entranceBox]) — слева или справа
 *    ([entranceSide]); стена с воротами разомкнута.
 *
 * Экран «Схема» показывает **весь корпус целиком** ([fullFragment]) — все ряды и
 * электролизёры, стены с единственным входом и дороги вдоль стен. При
 * необходимости схему можно разбить на фрагменты ([SchemeFragment]) — этим
 * пользуется экран постановки заявки, чтобы выбирать электролизёр крупно.
 *
 * Класс не зависит от Android/Compose и покрыт unit-тестами.
 */
class HallLayout(
    val rows: Int,
    val perRow: Int,
    val format: HallFormat,
) {
    companion object {
        /** Электролизёр в плане: длина × ширина (условные метры). */
        const val CELL_W = 1.0f
        const val CELL_H = 0.42f

        /**
         * Узкий зазор между электролизёрами одного блока — не проходим.
         * Используется только при [BLOCK_SIZE] > 1 (при 1 не задействован).
         */
        const val CELL_GAP = 0.16f

        /**
         * Сколько электролизёров в одном блоке. Значение 1 — группировки нет:
         * каждый электролизёр стоит отдельно, а между соседями — проход [BLOCK_GAP].
         */
        const val BLOCK_SIZE = 1

        /**
         * **Проезд техники** (МРГ/МПК): проход между соседними электролизёрами —
         * при [BLOCK_SIZE] = 1 между каждой парой. Уже самого электролизёра
         * ([CELL_W]), но достаточен, чтобы машина встала к нему.
         */
        const val BLOCK_GAP = 0.7f

        /**
         * **Центральная дорога** корпуса — широкая полоса между двумя линиями
         * электролизёров: место разворота техники к электролизёрам.
         */
        const val AISLE_H = 2.0f

        /**
         * Дорога вдоль боковых стен корпуса — узкая служебная полоса для выезда
         * к воротам (уже проезда техники [BLOCK_GAP]).
         */
        const val SIDE_LANE = 0.5f

        /** Дорога вдоль торцевых стен корпуса (сверху и снизу) — выезд к воротам. */
        const val TURN_LANE = 1.0f

        /** Ширина единственного входа в корпус. */
        const val ENTRANCE_W = 1.2f

        /** Отступ рамки блока вокруг электролизёров. */
        const val BLOCK_PAD = 0.12f

        /**
         * Минимальная ширина ячейки электролизёра на экране, dp.
         *
         * Схема корпуса не сжимается мельче этого размера: если весь корпус в
         * ширину не влезает — он **прокручивается по горизонтали**. Плотность
         * 320 dpi (xhdpi, 2 px на dp) даёт ячейку ≈100 px, по ней удобно
         * попадать пальцем (см. [SCHEME_MIN_TOUCH_DP]).
         */
        const val MIN_CELL_DP = 50f

        /** Сколько рядов показано одновременно (пара рядов + проезд между ними). */
        const val ROWS_PER_VIEW = 2

        /** Сколько электролизёров в ряду показано: на планшете больше, чем на телефоне. */
        const val COLS_TABLET = 7
        const val COLS_PHONE = 4

        fun colsFor(format: HallFormat): Int =
            if (format == HallFormat.TABLET) COLS_TABLET else COLS_PHONE

        fun of(scheme: Scheme, format: HallFormat): HallLayout =
            HallLayout(
                rows = scheme.rows.coerceAtLeast(1),
                perRow = scheme.perRow.coerceAtLeast(1),
                format = format,
            )
    }

    // ------------------------------------------------------------- размеры

    /** Шаг рядов по вертикали: электролизёр + проезд техники перед следующим рядом. */
    val rowPitch: Float = CELL_H + AISLE_H

    /** Ширина одного блока (при [BLOCK_SIZE] = 1 — одного электролизёра). */
    val blockWidth: Float = BLOCK_SIZE * CELL_W + (BLOCK_SIZE - 1) * CELL_GAP

    /** Сколько блоков в ряду (при [BLOCK_SIZE] = 1 — столько же, сколько электролизёров). */
    val blocksPerRow: Int = (perRow + BLOCK_SIZE - 1) / BLOCK_SIZE

    /** Размеры сетки электролизёров без дорог вдоль стен. */
    val gridWidth: Float = blocksPerRow * (blockWidth + BLOCK_GAP) - BLOCK_GAP

    /**
     * Высота сетки: два ряда электролизёров и **центральная дорога** [AISLE_H]
     * между ними. У последнего ряда проезда снизу уже нет — он снаружи сетки.
     */
    val gridHeight: Float = rows * rowPitch - AISLE_H

    /** Размеры всего корпуса. */
    val totalWidth: Float = 2 * SIDE_LANE + gridWidth
    val totalHeight: Float = 2 * TURN_LANE + gridHeight

    /** Сколько рядов и электролизёров в ряду помещается в один фрагмент. */
    val rowsPerView: Int = minOf(ROWS_PER_VIEW, rows)
    val colsPerView: Int = minOf(colsFor(format), perRow)

    /**
     * Размер условного метра схемы, dp.
     *
     * Схема занимает всю доступную ширину ([availableWidthDp]), но **не мельче**,
     * чем нужно для ячейки шириной [MIN_CELL_DP] (`CELL_W` — это и есть ячейка).
     * Если корпус шире экрана — схему прокручивают по горизонтали.
     */
    fun unitDp(availableWidthDp: Float, viewWidth: Float): Float =
        maxOf(availableWidthDp / viewWidth, MIN_CELL_DP / CELL_W)

    // ------------------------------------------------------------- геометрия

    private fun blockIndex(position: Int) = (position - 1) / BLOCK_SIZE

    /** Зазор справа от электролизёра `position`: внутри блока — узкий, между блоками — проход. */
    fun gapAfter(position: Int): Float =
        if (position % BLOCK_SIZE == 0 && position < perRow) BLOCK_GAP else CELL_GAP

    /** Зазор слева от электролизёра `position`. */
    fun gapBefore(position: Int): Float =
        if ((position - 1) % BLOCK_SIZE == 0 && position > 1) BLOCK_GAP else CELL_GAP

    /** Электролизёр (row, position — 1-based) в координатах корпуса. */
    fun cellBox(row: Int, position: Int): Box {
        val r = (row - 1).coerceIn(0, rows - 1)
        val p = (position - 1).coerceIn(0, perRow - 1)
        val b = blockIndex(p + 1)
        val i = p % BLOCK_SIZE
        return Box(
            x = SIDE_LANE + b * (blockWidth + BLOCK_GAP) + i * (CELL_W + CELL_GAP),
            y = TURN_LANE + r * rowPitch,
            w = CELL_W,
            h = CELL_H,
        )
    }

    /** Рамка блока, содержащего электролизёр `position` (по всей высоте ряда). */
    fun blockBox(row: Int, position: Int): Box {
        val r = (row - 1).coerceIn(0, rows - 1)
        val b = blockIndex((position - 1).coerceIn(0, perRow - 1) + 1)
        val cells = minOf(BLOCK_SIZE, perRow - b * BLOCK_SIZE)
        val w = cells * CELL_W + (cells - 1) * CELL_GAP
        return Box(
            x = SIDE_LANE + b * (blockWidth + BLOCK_GAP) - BLOCK_PAD,
            y = TURN_LANE + r * rowPitch - BLOCK_PAD,
            w = w + 2 * BLOCK_PAD,
            h = CELL_H + 2 * BLOCK_PAD,
        )
    }

    /** Индексы (0-based) всех блоков ряда, пересекающихся с диапазоном позиций. */
    fun blockIndexes(colFrom: Int, colTo: Int): IntRange {
        val from = blockIndex(colFrom.coerceIn(1, perRow))
        val to = blockIndex(colTo.coerceIn(1, perRow))
        return from..to
    }

    /** Номер первого электролизёра (1-based) в блоке `block` (0-based). */
    fun firstPositionOfBlock(block: Int): Int = block * BLOCK_SIZE + 1

    /** Высота полосы ряда: сам ряд и центральная дорога под ним (у последнего ряда дороги нет). */
    private fun bandHeight(row: Int): Float =
        CELL_H + if (row >= rows) 0f else AISLE_H

    /** Полоса ряда: электролизёры и центральная дорога перед следующим рядом. */
    fun rowBandBox(row: Int): Box {
        val r = (row - 1).coerceIn(0, rows - 1)
        return Box(SIDE_LANE, TURN_LANE + r * rowPitch, gridWidth, bandHeight(row))
    }

    /** Центральная дорога корпуса — под рядом `row` (у последнего ряда её нет). */
    fun aisleBox(row: Int): Box {
        val r = (row - 1).coerceIn(0, rows - 1)
        return Box(SIDE_LANE, TURN_LANE + r * rowPitch + CELL_H, gridWidth, bandHeight(row) - CELL_H)
    }

    /**
     * Проход между блоками перед электролизёром `position`
     * (`position` — начало блока). Связывает ряд с центральной дорогой под ним.
     */
    fun passageBox(row: Int, position: Int): Box {
        val cell = cellBox(row, position)
        val r = (row - 1).coerceIn(0, rows - 1)
        return Box(cell.x - BLOCK_GAP, TURN_LANE + r * rowPitch, BLOCK_GAP, bandHeight(row))
    }

    /** Дороги вдоль торцевых стен корпуса (сверху и снизу). */
    fun turnLanes(): List<Box> = listOf(
        Box(0f, 0f, totalWidth, TURN_LANE),
        Box(0f, totalHeight - TURN_LANE, totalWidth, TURN_LANE),
    )

    /** Дороги вдоль боковых стен корпуса (параллельно стене, слева и справа). */
    fun sideLanes(): List<Box> = listOf(
        Box(0f, TURN_LANE, SIDE_LANE, gridHeight),
        Box(totalWidth - SIDE_LANE, TURN_LANE, SIDE_LANE, gridHeight),
    )

    /** Граница половин ряда A/B: x-координата, если ряд делится пополам. */
    fun halfBoundaryX(halfSize: Int): Float {
        val lastOfA = cellBox(1, halfSize.coerceIn(1, perRow))
        return lastOfA.right + gapAfter(halfSize.coerceIn(1, perRow)) / 2f
    }

    /**
     * Опасные зоны и тупики приходят с сервера в нормализованных координатах
     * (0..1) по сетке корпуса — переводим их в координаты корпуса.
     */
    fun zoneBox(x: Float, y: Float, w: Float, h: Float): Box = Box(
        x = SIDE_LANE + x * gridWidth,
        y = TURN_LANE + y * gridHeight,
        w = w * gridWidth,
        h = h * gridHeight,
    )

    /** Ворота приходят с сервера в нормализованных координатах (0..1). */
    fun gateCenter(x: Float, y: Float): Pair<Float, Float> =
        SIDE_LANE + x * gridWidth to TURN_LANE + y * gridHeight

    // ------------------------------------------------------------- вход

    /** Сторона единственного входа по положению ворот (левая половина — слева). */
    fun entranceSide(gateX: Float): HallSide =
        if (gateX < 0.5f) HallSide.LEFT else HallSide.RIGHT

    /** Прямоугольник входа в стене корпуса на высоте `cy`. */
    fun entranceBox(side: HallSide, cy: Float): Box = when (side) {
        HallSide.LEFT -> Box(0f, cy - ENTRANCE_W / 2f, SIDE_LANE, ENTRANCE_W)
        HallSide.RIGHT -> Box(totalWidth - SIDE_LANE, cy - ENTRANCE_W / 2f, SIDE_LANE, ENTRANCE_W)
    }

    // ------------------------------------------------------------- фрагмент

    /** Весь корпус целиком: все ряды и все электролизёры в них. */
    fun fullFragment(): SchemeFragment = SchemeFragment(1, rows, 1, perRow)

    /** Фрагмент, покрывающий электролизёр (row, position). */
    fun fragmentAround(row: Int, position: Int): SchemeFragment {
        val rowTo = row.coerceIn(1, rows).let { it + rowsPerView - 1 }.coerceAtMost(rows)
        val rowFrom = (rowTo - rowsPerView + 1).coerceAtLeast(1)
        val colTo = position.coerceIn(1, perRow).let { it + colsPerView - 1 }.coerceAtMost(perRow)
        val colFrom = (colTo - colsPerView + 1).coerceAtLeast(1)
        return SchemeFragment(rowFrom, rowTo, colFrom, colTo)
    }

    /** Приводит произвольный фрагмент к допустимым границам корпуса. */
    fun clamp(fragment: SchemeFragment): SchemeFragment {
        val rowTo = fragment.rowTo.coerceIn(1, rows)
        val rowFrom = fragment.rowFrom.coerceIn(1, rowTo)
        val colTo = fragment.colTo.coerceIn(1, perRow)
        val colFrom = fragment.colFrom.coerceIn(1, colTo)
        return SchemeFragment(rowFrom, rowTo, colFrom, colTo)
    }

    /** Прямоугольник корпуса, попадающий в видимую область фрагмента. */
    fun viewBox(fragment: SchemeFragment): Box {
        val f = clamp(fragment)
        val first = cellBox(1, f.colFrom)
        val last = cellBox(1, f.colTo)
        val left = if (f.colFrom <= 1) 0f else first.x - gapBefore(f.colFrom) / 2f
        val right = if (f.colTo >= perRow) totalWidth else last.right + gapAfter(f.colTo) / 2f
        val top = if (f.rowFrom <= 1) 0f
        else TURN_LANE + (f.rowFrom - 1) * rowPitch - AISLE_H
        val bottom = if (f.rowTo >= rows) totalHeight
        else TURN_LANE + f.rowTo * rowPitch
        return Box(left, top, (right - left).coerceAtLeast(0.01f), (bottom - top).coerceAtLeast(0.01f))
    }

    /** Сдвиг фрагмента по рядам (delta — число «экранов»). */
    fun shiftRows(fragment: SchemeFragment, delta: Int): SchemeFragment {
        val f = clamp(fragment)
        val maxFrom = (rows - rowsPerView + 1).coerceAtLeast(1)
        val rowFrom = (f.rowFrom + delta * rowsPerView).coerceIn(1, maxFrom)
        return f.copy(rowFrom = rowFrom, rowTo = (rowFrom + rowsPerView - 1).coerceAtMost(rows))
    }

    /** Сдвиг фрагмента вдоль ряда (delta — число «экранов»). */
    fun shiftCols(fragment: SchemeFragment, delta: Int): SchemeFragment {
        val f = clamp(fragment)
        val maxFrom = (perRow - colsPerView + 1).coerceAtLeast(1)
        val colFrom = (f.colFrom + delta * colsPerView).coerceIn(1, maxFrom)
        return f.copy(colFrom = colFrom, colTo = (colFrom + colsPerView - 1).coerceAtMost(perRow))
    }

    fun canShiftRows(fragment: SchemeFragment, delta: Int): Boolean =
        shiftRows(fragment, delta) != clamp(fragment)

    fun canShiftCols(fragment: SchemeFragment, delta: Int): Boolean =
        shiftCols(fragment, delta) != clamp(fragment)
}
