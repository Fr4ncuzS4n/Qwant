package com.example.qwant.server

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseTest {

    private val dbs = ArrayList<Db>()

    private fun fresh(count: Int = 150): Db = Db(electrolyzerCount = count).also { dbs += it }

    @AfterTest
    fun cleanup() {
        // соединения закрываем по завершении процесса; файловых БД здесь нет
    }

    // ------------------------------------------------------------- наполнение

    @Test
    fun `150 electrolyzers are laid out in two lines of 75, split in half`() {
        val db = fresh(150)
        val s = db.scheme()
        assertEquals(2, s.rows)
        assertEquals(75, s.perRow)
        assertEquals(37, s.halfSize)
        assertEquals(150, s.electrolyzers.size)
    }

    @Test
    fun `100 electrolyzers are laid out in two lines of 50, split in half`() {
        val db = fresh(100)
        val s = db.scheme()
        assertEquals(2, s.rows)
        assertEquals(50, s.perRow)
        assertEquals(25, s.halfSize)
        assertEquals(100, s.electrolyzers.size)
    }

    @Test
    fun `first half is A, second half is B`() {
        val db = fresh(100)
        val els = db.scheme().electrolyzers
        assertEquals("A", els.first { it.row == 1 && it.position == 1 }.half)
        assertEquals("A", els.first { it.row == 1 && it.position == 25 }.half)
        assertEquals("B", els.first { it.row == 1 && it.position == 26 }.half)
        assertEquals("B", els.first { it.row == 1 && it.position == 50 }.half)
    }

    @Test
    fun `electrolyzers are numbered sequentially from 1`() {
        val db = fresh(100)
        val numbers = db.scheme().electrolyzers.map { it.number }
        assertEquals((1..100).toList(), numbers)
    }

    @Test
    fun `sample data is seeded`() {
        val db = fresh()
        assertEquals(4, db.equipment().size)
        assertEquals(4, db.drivers().size)
        assertEquals(7, db.tags().size)
        assertTrue(db.gates().isNotEmpty())
        assertTrue(db.scheme().zones.any { it.kind == ZoneKind.DANGER })
        assertTrue(db.scheme().zones.any { it.kind == ZoneKind.DEAD_END })
        assertTrue(db.orders().isNotEmpty())
    }

    // --------------------------------------------------------------- корпуса

    @Test
    fun `two buildings share the same layout but have different entrances`() {
        val db = fresh(150)
        val buildings = db.buildings()
        assertEquals(4, buildings.size)
        assertEquals(Db.DEFAULT_BUILDING, buildings.first().id)
        assertTrue(buildings.first().isDefault)

        val b4 = db.scheme("4")
        val b3 = db.scheme("3")
        assertEquals(150, b4.electrolyzerCount)
        assertEquals(150, b3.electrolyzerCount)
        assertEquals(75, b4.perRow)
        assertEquals(75, b3.perRow)
        assertEquals((1..150).toList(), b4.electrolyzers.map { it.number })
        assertEquals((1..150).toList(), b3.electrolyzers.map { it.number })

        // у каждого корпуса свои зоны и единственный вход
        assertTrue(b4.zones.isNotEmpty() && b3.zones.isNotEmpty())
        assertEquals(1, b4.gates.size)
        assertEquals(1, b3.gates.size)
        assertTrue(b4.gates.first().x < 0.5f)
        assertTrue(b3.gates.first().x >= 0.5f)
    }

    @Test
    fun `scheme defaults to the default building`() {
        val db = fresh()
        assertEquals(Db.DEFAULT_BUILDING, db.scheme().building)
    }

    @Test
    fun `unknown building is not reported as existing`() {
        val db = fresh()
        assertFalse(db.buildingExists("99"))
        assertTrue(db.buildingExists(Db.DEFAULT_BUILDING))
    }

    // ----------------------------------------------------------- жизненный цикл

    @Test
    fun `accept then complete an order and accrue motohours`() {
        val db = fresh()
        val newOrder = db.orders(status = OrderStatus.NEW).first()
        val before = db.drivers().first { it.id == 1 }.equipmentId.let { db.equipmentById(it!!)!!.motohours }

        val accepted = db.acceptOrder(newOrder.id, driverId = 1)!!
        assertEquals(OrderStatus.ACCEPTED, accepted.status)
        assertEquals(1, accepted.driverId)
        assertNotNull(accepted.acceptedAtMillis)

        val done = db.completeOrder(newOrder.id, deviationTagId = 1, comment = "ок", motohoursDelta = 3.0)!!
        assertEquals(OrderStatus.DONE, done.status)
        assertEquals("Неисправность узла электролизёра", done.deviationTagLabel)

        val after = db.equipmentById(db.drivers().first { it.id == 1 }.equipmentId!!)!!.motohours
        assertEquals(before + 3.0, after, 1e-9)
    }

    @Test
    fun `accepted podsypka order gets a traffic light`() {
        val db = fresh()
        // в образцовых данных есть заявка, принятая 12 минут назад
        val accepted = db.orders().first { it.status == OrderStatus.ACCEPTED && it.type == OrderType.PODSYPKA }
        assertNotNull(accepted.urgency)
        assertTrue(accepted.urgency == Urgency.ORANGE || accepted.urgency == Urgency.RED)
        assertNotNull(accepted.elapsedMinutes)
    }

    @Test
    fun `re-accepting a done order does not change it`() {
        val db = fresh()
        val done = db.orders().first { it.status == OrderStatus.DONE }
        val again = db.acceptOrder(done.id, driverId = 2)!!
        assertEquals(OrderStatus.DONE, again.status)
    }

    @Test
    fun `create order sets new status`() {
        val db = fresh()
        val o = db.createOrder(OrderType.PODSYPKA, electrolyzerId = 1, description = "тест", requestedBy = "Диспетчер", isRepair = false)
        assertEquals(OrderStatus.NEW, o.status)
        assertEquals(OrderType.PODSYPKA, o.type)
        assertEquals(1, o.electrolyzerId)
    }

    @Test
    fun `gate can be opened remotely`() {
        val db = fresh()
        val gate = db.gates().first()
        assertTrue(!gate.isOpen)
        val opened = db.setGateOpen(gate.id, true)!!
        assertTrue(opened.isOpen)
    }

    @Test
    fun `stats aggregate motohours by type`() {
        val db = fresh()
        val stats = db.stats()
        assertTrue(stats.totalMotohours > 0)
        assertEquals(setOf(EquipmentType.MRG, EquipmentType.MPK), stats.byType.keys)
    }

    @Test
    fun `file database persists across instances`() {
        val file = File.createTempFile("qwant-test", ".db")
        file.delete()
        try {
            Db("${file.absolutePath}").addMotohours(1, 5.0)
            val reopened = Db(file.absolutePath)
            assertEquals(105.0, reopened.equipmentById(1)!!.motohours, 1e-9)
        } finally {
            file.delete()
        }
    }
}
