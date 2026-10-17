package com.example.qwant.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.qwant.data.Gate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Юнит-тесты попадания пальцем по схеме корпуса ([nearestTouch]).
 *
 * Ячейка электролизёра на плане мелкая, поэтому тап вне контура ячейки должен
 * «прилипать» к ближайшему электролизёру, а не пропадать впустую; но и тап,
 * далёкий от всей схемы, ничего не выбирает.
 */
class SchemeTapTest {

    /** Минимальная зона касания — как на экране (`SCHEME_MIN_TOUCH_DP.dp`). */
    private val minTouch = 48f

    /** Ряд мелких ячеек: 20×10, чтобы «промах» пальцем был заметен. */
    private fun cell(x: Float) = Rect(x, 0f, x + 20f, 10f)

    @Test
    fun `tap on the cell selects it`() {
        val zones = listOf(cell(100f) to "cell")

        assertEquals("cell", nearestTouch(zones, Offset(110f, 5f), minTouch))
    }

    @Test
    fun `tap just outside a small cell still selects it`() {
        val zones = listOf(cell(0f) to "cell")

        // палец промахнулся мимо ячейки на 10 px — в пределах увеличенной зоны
        assertEquals("cell", nearestTouch(zones, Offset(30f, 5f), minTouch))
    }

    @Test
    fun `tap between two neighbours snaps to the nearest one`() {
        val zones = listOf(cell(0f) to "left", cell(30f) to "right")

        assertEquals("right", nearestTouch(zones, Offset(28f, 5f), minTouch))
        assertEquals("left", nearestTouch(zones, Offset(22f, 5f), minTouch))
    }

    @Test
    fun `tap far from every cell selects nothing`() {
        val zones = listOf(cell(0f) to "cell")

        // 25 px от ячейки — уже за пределами увеличенной зоны (24 px)
        assertNull(nearestTouch(zones, Offset(45f, 5f), minTouch))
        assertNull(nearestTouch(zones, Offset(500f, 500f), minTouch))
    }

    @Test
    fun `no zones means no selection`() {
        assertNull(nearestTouch(emptyList<Pair<Rect, String>>(), Offset(10f, 10f), minTouch))
    }

    @Test
    fun `on equal distance the nearer centre wins`() {
        // обе зоны на одинаковом расстоянии 10, но центры — на разном
        val near = Rect(10f, -5f, 10f, 5f)
        val far = Rect(10f, -5f, 30f, 5f)

        // «far» стоит первой в списке — выбор не должен зависеть от порядка
        val zones = listOf(far to "far", near to "near")

        assertEquals("near", nearestTouch(zones, Offset(0f, 0f), minTouch))
    }

    @Test
    fun `gates are matched the same way as electrolyzers`() {
        val gate = Gate(id = 7, name = "Ворота", x = 0f, y = 0f)
        val zones = listOf(Rect(200f, 200f, 234f, 234f) to gate)

        // тап мимо ворот, но в пределах увеличенной зоны
        assertEquals(gate, nearestTouch(zones, Offset(190f, 217f), minTouch))
        assertNull(nearestTouch(zones, Offset(100f, 100f), minTouch))
    }
}
