package com.example.qwant.ui

import com.example.qwant.data.Scheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты геометрии схемы корпуса: электролизёры стоят двумя линиями (по
 * одной с каждой стороны центральной дороги) по отдельности — между соседями
 * проход для техники. По центру корпуса — широкая дорога разворота, вдоль стен —
 * дороги для выезда к воротам, а в стене корпуса — единственный вход. Экран
 * схемы показывает весь корпус целиком ([HallLayout.fullFragment]).
 */
class SchemeLayoutTest {

    private companion object {
        /** Типичная плотность телефона, px на dp (320 dpi). */
        const val DENSITY_320 = 2f
    }

    private fun scheme(rows: Int = 5, perRow: Int = 30) = Scheme(
        building = "4",
        rows = rows,
        perRow = perRow,
        halfSize = perRow / 2,
        electrolyzerCount = rows * perRow,
        electrolyzers = emptyList(),
        zones = emptyList(),
        gates = emptyList(),
    )

    // ------------------------------------------------------- формат и размер

    @Test
    fun `phone shows fewer electrolyzers per row than tablet`() {
        val phone = HallLayout.of(scheme(), HallFormat.PHONE)
        val tablet = HallLayout.of(scheme(), HallFormat.TABLET)

        assertEquals(HallLayout.COLS_PHONE, phone.colsPerView)
        assertEquals(HallLayout.COLS_TABLET, tablet.colsPerView)
        assertTrue(tablet.colsPerView > phone.colsPerView)
    }

    @Test
    fun `visible columns never exceed electrolyzers in a row`() {
        val layout = HallLayout.of(scheme(perRow = 3), HallFormat.TABLET)
        assertEquals(3, layout.colsPerView)
    }

    // ------------------------------------------------------- масштаб на экране

    @Test
    fun `electrolyzer cell is never smaller than the minimum size`() {
        val layout = HallLayout.of(scheme(rows = 5, perRow = 30), HallFormat.PHONE)
        val view = layout.viewBox(layout.fullFragment())

        // узкий экран телефона: корпус не влезает — схема держит минимальную ячейку
        val unit = layout.unitDp(availableWidthDp = 360f, viewWidth = view.w)

        assertEquals(HallLayout.MIN_CELL_DP, unit, 0.001f)
        assertEquals(HallLayout.MIN_CELL_DP, unit * HallLayout.CELL_W, 0.001f)
        // ≈100 px на телефоне с плотностью 2 px/dp — по ней попадает палец
        assertEquals(100f, unit * HallLayout.CELL_W * DENSITY_320, 0.001f)
    }

    @Test
    fun `a small hall is stretched to the available width`() {
        val layout = HallLayout.of(scheme(rows = 1, perRow = 2), HallFormat.TABLET)
        val view = layout.viewBox(layout.fullFragment())
        val unit = layout.unitDp(availableWidthDp = 360f, viewWidth = view.w)

        // корпус уже экрана: ячейки крупнее минимальной, схема занимает всю ширину
        assertTrue(unit > HallLayout.MIN_CELL_DP)
        assertEquals(360f, unit * view.w, 0.001f)
    }

    @Test
    fun `unit never drops below the cell minimum for any fragment`() {
        val layout = HallLayout.of(scheme(rows = 5, perRow = 30), HallFormat.PHONE)

        (1..5).forEach { row ->
            (1..30 step 7).forEach { position ->
                val view = layout.viewBox(layout.fragmentAround(row, position))
                val unit = layout.unitDp(availableWidthDp = 220f, viewWidth = view.w)
                assertTrue(unit * HallLayout.CELL_W >= HallLayout.MIN_CELL_DP - 0.001f)
            }
        }
    }

    @Test
    fun `fragment never shows all rows of a big hall`() {
        val layout = HallLayout.of(scheme(rows = 5), HallFormat.TABLET)
        assertEquals(HallLayout.ROWS_PER_VIEW, layout.rowsPerView)
        assertTrue(layout.rowsPerView < 5)
    }

    // ------------------------------------------- электролизёры и проходы

    @Test
    fun `electrolyzers stand individually with a passage between them`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)

        // BLOCK_SIZE = 1: группировки нет — каждый электролизёр отдельно,
        // между любыми соседями полноценный проход, а не служебный зазор
        assertEquals(1, HallLayout.BLOCK_SIZE)
        (1 until layout.perRow).forEach { position ->
            val gap = layout.cellBox(1, position + 1).x - layout.cellBox(1, position).right
            assertEquals(HallLayout.BLOCK_GAP, gap, 0.001f)
        }
        assertTrue(HallLayout.BLOCK_GAP > HallLayout.CELL_GAP)
    }

    @Test
    fun `blocks are separated by a passage wide enough for a machine`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val lastOfBlock = layout.cellBox(1, HallLayout.BLOCK_SIZE)
        val firstOfNext = layout.cellBox(1, HallLayout.BLOCK_SIZE + 1)

        assertEquals(HallLayout.BLOCK_GAP, firstOfNext.x - lastOfBlock.right, 0.001f)
        // проход заметно шире служебного зазора и не уже дороги вдоль стены
        assertTrue(HallLayout.BLOCK_GAP > HallLayout.CELL_GAP)
        assertTrue(HallLayout.BLOCK_GAP >= HallLayout.SIDE_LANE)
    }

    @Test
    fun `passage is narrower than the electrolyzer itself`() {
        // электролизёр — крупный агрегат: в плане он шире, чем проезд между соседями
        assertTrue(HallLayout.BLOCK_GAP < HallLayout.CELL_W)
        // при этом проезд шире служебного зазора и равен дороге вдоль стены
        assertEquals(HallLayout.SIDE_LANE, HallLayout.BLOCK_GAP, 0.001f)
    }

    @Test
    fun `electrolyzer is a big enough touch target`() {
        // 50 dp ≈ 100 px на плотности 320 — по ячейке уверенно попадает палец
        val layout = HallLayout.of(scheme(), HallFormat.PHONE)
        val fullView = layout.viewBox(layout.fullFragment())

        val unit = layout.unitDp(availableWidthDp = 360f, viewWidth = fullView.w)
        val cellPx = unit * HallLayout.CELL_W * DENSITY_320

        assertTrue(cellPx >= 96f)
    }

    @Test
    fun `block frame wraps all the electrolyzers of its block`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val first = layout.firstPositionOfBlock(1)
        val block = layout.blockBox(row = 1, position = first)

        (first until first + HallLayout.BLOCK_SIZE).forEach { position ->
            val cell = layout.cellBox(1, position)
            assertTrue(cell.x >= block.x - 0.001f)
            assertTrue(cell.right <= block.right + 0.001f)
        }
        // следующий блок в рамку не попадает
        assertTrue(layout.cellBox(1, first + HallLayout.BLOCK_SIZE).x > block.right)
    }

    @Test
    fun `passage between blocks connects the aisles of both rows`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val passage = layout.passageBox(row = 1, position = HallLayout.BLOCK_SIZE + 1)
        val rowBand = layout.rowBandBox(1)
        val aisle = layout.aisleBox(1)

        // проход идёт от ряда до низа проезда под ним — техника проходит с ряда на ряд
        assertEquals(rowBand.y, passage.y, 0.001f)
        assertEquals(rowBand.bottom, passage.bottom, 0.001f)
        assertEquals(HallLayout.BLOCK_GAP, passage.w, 0.001f)
        // и начинается сразу после блока
        assertTrue(passage.right <= layout.cellBox(1, HallLayout.BLOCK_SIZE + 1).x + 0.001f)
        assertTrue(aisle.bottom <= layout.cellBox(2, 1).y + 0.001f)
    }

    @Test
    fun `every electrolyzer in a row is its own block`() {
        val layout = HallLayout.of(scheme(perRow = 30), HallFormat.TABLET)

        // BLOCK_SIZE = 1: блоков ровно столько же, сколько электролизёров
        assertEquals(30, layout.blocksPerRow)
        assertEquals(30, layout.firstPositionOfBlock(29))
        // последний электролизёр не вылезает за ширину сетки
        assertTrue(layout.cellBox(1, 30).right <= layout.gridWidth + HallLayout.SIDE_LANE + 0.001f)
    }

    // ---------------------------------------------------- проезды и разворот

    @Test
    fun `there is a drive aisle between neighbouring rows`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val row1 = layout.cellBox(1, 1)
        val row2 = layout.cellBox(2, 1)
        val aisle = layout.aisleBox(1)

        // проезд начинается под электролизёрами первого ряда и не пересекает второй ряд
        assertEquals(row1.bottom, aisle.y, 0.001f)
        assertTrue(aisle.h >= HallLayout.AISLE_H - 0.001f)
        assertTrue(aisle.bottom <= row2.y + 0.001f)
    }

    @Test
    fun `wide turn lanes run along the top and the bottom of the house`() {
        val layout = HallLayout.of(scheme(), HallFormat.PHONE)
        val (top, bottom) = layout.turnLanes()

        assertEquals(0f, top.y, 0.001f)
        assertEquals(layout.totalWidth, top.w, 0.001f)
        assertEquals(layout.totalHeight, bottom.bottom, 0.001f)
        // дорога вдоль торца не уже боковой — техника проезжает к воротам
        assertTrue(HallLayout.TURN_LANE >= HallLayout.SIDE_LANE)
        assertTrue(layout.cellBox(1, 1).y >= top.bottom - 0.001f)
    }

    @Test
    fun `wide central road runs between the two electrolyzer lines`() {
        val layout = HallLayout.of(scheme(rows = 2, perRow = 75), HallFormat.TABLET)
        val (top, bottom) = layout.turnLanes()

        // центральная дорога — самая широкая: по ней разворачиваются к электролизёрам
        assertTrue(HallLayout.AISLE_H > HallLayout.TURN_LANE)

        // дорога ровно одна, лежит между двумя линиями и совпадает с центром корпуса
        val central = layout.aisleBox(1)
        assertEquals(layout.cellBox(1, 1).bottom, central.y, 0.001f)
        assertEquals(HallLayout.AISLE_H, central.h, 0.001f)
        assertEquals((top.bottom + bottom.y) / 2f, central.y + central.h / 2f, 0.001f)
    }

    @Test
    fun `the second line sits against the far end road`() {
        val layout = HallLayout.of(scheme(rows = 2, perRow = 75), HallFormat.TABLET)

        // у последней линии центральной дороги под ней нет — сетка кончается линией
        assertEquals(0f, layout.aisleBox(2).h, 0.001f)
        assertEquals(layout.turnLanes()[1].y, layout.cellBox(2, 1).bottom, 0.001f)
    }

    @Test
    fun `house has a drive lane along each side wall`() {
        val layout = HallLayout.of(scheme(), HallFormat.PHONE)
        val (left, right) = layout.sideLanes()

        assertEquals(0f, left.x, 0.001f)
        assertEquals(HallLayout.SIDE_LANE, left.w, 0.001f)
        assertEquals(layout.totalWidth, right.right, 0.001f)
        assertEquals(layout.turnLanes()[0].bottom, left.y, 0.001f)
    }

    @Test
    fun `all electrolyzers stay inside the house bounds`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val last = layout.cellBox(layout.rows, layout.perRow)

        assertTrue(last.right <= layout.totalWidth + 0.001f)
        assertTrue(last.bottom <= layout.totalHeight + 0.001f)
        assertTrue(layout.cellBox(1, 1).x >= HallLayout.SIDE_LANE - 0.001f)
        assertTrue(layout.cellBox(1, 1).y >= HallLayout.TURN_LANE - 0.001f)
    }

    @Test
    fun `half boundary lies between the two halves of a row`() {
        val layout = HallLayout.of(scheme(perRow = 30), HallFormat.PHONE)
        val lastOfA = layout.cellBox(1, 15)
        val firstOfB = layout.cellBox(1, 16)
        val boundary = layout.halfBoundaryX(15)

        assertTrue(boundary > lastOfA.x)
        assertTrue(boundary < firstOfB.right)
    }

    // ------------------------------------------------------------- вход

    @Test
    fun `entrance side follows the gate position`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)

        assertEquals(HallSide.LEFT, layout.entranceSide(0.0f))
        assertEquals(HallSide.RIGHT, layout.entranceSide(1.0f))
    }

    @Test
    fun `entrance is a gap in the wall on the chosen side only`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val cy = layout.totalHeight / 2f

        val left = layout.entranceBox(HallSide.LEFT, cy)
        assertEquals(0f, left.x, 0.001f)
        assertEquals(HallLayout.SIDE_LANE, left.w, 0.001f)
        assertTrue(left.h >= HallLayout.ENTRANCE_W - 0.001f)

        val right = layout.entranceBox(HallSide.RIGHT, cy)
        assertEquals(layout.totalWidth, right.right, 0.001f)
    }

    // ----------------------------------------------------------- фрагмент

    @Test
    fun `fragment around a cell contains that cell`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val fragment = layout.fragmentAround(3, 20)

        assertTrue(3 in fragment.rowFrom..fragment.rowTo)
        assertTrue(20 in fragment.colFrom..fragment.colTo)
        assertEquals(layout.rowsPerView, fragment.rows)
        assertEquals(layout.colsPerView, fragment.cols)
    }

    @Test
    fun `fragment is clamped near the far corner`() {
        val layout = HallLayout.of(scheme(rows = 5, perRow = 30), HallFormat.TABLET)
        val fragment = layout.fragmentAround(5, 30)

        assertEquals(5, fragment.rowTo)
        assertEquals(30, fragment.colTo)
        assertTrue(fragment.rowFrom >= 1)
        assertTrue(fragment.colFrom >= 1)
    }

    @Test
    fun `view box covers every electrolyzer of the fragment`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val fragment = layout.fragmentAround(3, 20)
        val view = layout.viewBox(fragment)

        (fragment.rowFrom..fragment.rowTo).forEach { row ->
            (fragment.colFrom..fragment.colTo).forEach { position ->
                val cell = layout.cellBox(row, position)
                assertTrue(cell.x >= view.x - 0.001f)
                assertTrue(cell.right <= view.right + 0.001f)
                assertTrue(cell.y >= view.y - 0.001f)
                assertTrue(cell.bottom <= view.bottom + 0.001f)
            }
        }
    }

    @Test
    fun `view box includes the turn lane when the fragment touches the wall`() {
        val layout = HallLayout.of(scheme(), HallFormat.PHONE)
        val view = layout.viewBox(layout.fragmentAround(1, 1))

        assertEquals(0f, view.x, 0.001f)
        assertEquals(0f, view.y, 0.001f)
    }

    @Test
    fun `full fragment covers the whole house`() {
        val layout = HallLayout.of(scheme(rows = 5, perRow = 30), HallFormat.PHONE)
        val full = layout.fullFragment()

        assertEquals(1, full.rowFrom)
        assertEquals(5, full.rowTo)
        assertEquals(1, full.colFrom)
        assertEquals(30, full.colTo)

        // вид на весь корпус совпадает с габаритами корпуса целиком
        val view = layout.viewBox(full)
        assertEquals(0f, view.x, 0.001f)
        assertEquals(0f, view.y, 0.001f)
        assertEquals(layout.totalWidth, view.right, 0.001f)
        assertEquals(layout.totalHeight, view.bottom, 0.001f)
    }

    // ------------------------------------------------------------ навигация

    @Test
    fun `fragment can be moved along a row and between row pairs`() {
        val layout = HallLayout.of(scheme(), HallFormat.PHONE)
        val start = layout.fragmentAround(1, 1)

        val right = layout.shiftCols(start, 1)
        assertEquals(start.colFrom + layout.colsPerView, right.colFrom)
        assertFalse(layout.canShiftCols(start, -1))

        val down = layout.shiftRows(start, 1)
        assertEquals(start.rowFrom + layout.rowsPerView, down.rowFrom)
        assertFalse(layout.canShiftRows(start, -1))
    }

    @Test
    fun `moving past the far edge stops at the last fragment`() {
        val layout = HallLayout.of(scheme(rows = 5, perRow = 30), HallFormat.TABLET)
        var fragment = layout.fragmentAround(1, 1)

        repeat(20) { fragment = layout.shiftCols(fragment, 1) }
        assertEquals(30, fragment.colTo)
        assertFalse(layout.canShiftCols(fragment, 1))

        repeat(20) { fragment = layout.shiftRows(fragment, 1) }
        assertEquals(5, fragment.rowTo)
        assertFalse(layout.canShiftRows(fragment, 1))
    }

    @Test
    fun `clamp fixes an out of range fragment`() {
        val layout = HallLayout.of(scheme(rows = 5, perRow = 30), HallFormat.PHONE)
        val clamped = layout.clamp(SchemeFragment(rowFrom = 0, rowTo = 99, colFrom = -5, colTo = 99))

        assertTrue(clamped.rowFrom >= 1)
        assertTrue(clamped.rowTo <= 5)
        assertTrue(clamped.colFrom >= 1)
        assertTrue(clamped.colTo <= 30)
    }

    // ------------------------------------------------------- зоны и ворота

    @Test
    fun `zone coordinates are mapped inside the electrolyzer grid`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val zone = layout.zoneBox(x = 0f, y = 0f, w = 1f, h = 1f)

        assertEquals(HallLayout.SIDE_LANE, zone.x, 0.001f)
        assertEquals(HallLayout.TURN_LANE, zone.y, 0.001f)
        assertEquals(layout.gridWidth, zone.w, 0.001f)
        assertEquals(layout.gridHeight, zone.h, 0.001f)
    }

    @Test
    fun `gate center is mapped into the house`() {
        val layout = HallLayout.of(scheme(), HallFormat.TABLET)
        val (x, y) = layout.gateCenter(1f, 1f)

        assertTrue(x <= layout.totalWidth + 0.001f)
        assertTrue(y <= layout.totalHeight + 0.001f)
        assertTrue(x > 0f && y > 0f)
    }
}
