package com.example.qwant.server

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Instant

/**
 * Доступ к базе данных SQLite (см. AGENTS.md: «для базы данных используй sqlite3»).
 *
 * По умолчанию — образная (демонстрационная) база в памяти с образцами данных,
 * которая заполняется при старте. Чтобы сохранять данные между запусками,
 * задайте путь к файлу: `QWANT_DB=/path/to/qwant.db`.
 *
 * Количество электролизёров задаётся через `QWANT_ELECTROLYZERS` (100 или 150),
 * по умолчанию 150. Электролизёры располагаются параллельными рядами и делятся
 * пополам (половина A — левая, B — правая).
 */
class Db(
    private val dbPath: String = System.getenv("QWANT_DB") ?: ":memory:",
    private val electrolyzerCount: Int = (System.getenv("QWANT_ELECTROLYZERS")?.toIntOrNull() ?: 150),
    private val rows: Int = 5,
) {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

    /** Номер корпуса (цеха). */
    private val BUILDING = "4"

    /** Электролизёров в одном ряду. */
    val perRow: Int = electrolyzerCount / rows

    /** Размер половины ряда (ряд делится пополам — «подленные пополам»). */
    val halfSize: Int = perRow / 2

    init {
        conn.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        createSchema()
        if (countOf("equipment") == 0) seed()
    }

    // ---------------------------------------------------------------- schema

    private fun createSchema() = conn.createStatement().use { st ->
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS equipment (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                model TEXT NOT NULL,
                motohours REAL NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'READY'
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS drivers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                equipment_id INTEGER REFERENCES equipment(id)
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS electrolyzers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number INTEGER NOT NULL,
                building TEXT NOT NULL,
                row INTEGER NOT NULL,
                position INTEGER NOT NULL,
                half TEXT NOT NULL,
                series TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'OK'
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS zones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                label TEXT NOT NULL,
                building TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                w REAL NOT NULL,
                h REAL NOT NULL
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS gates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                building TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                is_open INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS deviation_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                label TEXT NOT NULL
            )
            """.trimIndent()
        )
        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                electrolyzer_id INTEGER REFERENCES electrolyzers(id),
                driver_id INTEGER REFERENCES drivers(id),
                requested_by TEXT,
                deviation_tag_id INTEGER REFERENCES deviation_tags(id),
                comment TEXT,
                is_repair INTEGER NOT NULL DEFAULT 0,
                motohours_delta REAL,
                created_at INTEGER NOT NULL,
                accepted_at INTEGER,
                completed_at INTEGER
            )
            """.trimIndent()
        )
    }

    private fun countOf(table: String): Int =
        conn.createStatement().use { st ->
            st.executeQuery("SELECT COUNT(*) FROM $table").use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }

    // ------------------------------------------------------------------ seed

    private fun seed() {
        val now = System.currentTimeMillis()

        // --- техника (дизельные машины) ---
        val equipment = listOf(
            Triple("МРГ-1", EquipmentType.MRG, "С8БМ"),
            Triple("МРГ-2", EquipmentType.MRG, "С8БМ(Э)"),
            Triple("МПК-1", EquipmentType.MPK, "С8БМ"),
            Triple("МПК-2", EquipmentType.MPK, "С3"),
        )
        equipment.forEachIndexed { i, (name, type, model) ->
            exec(
                "INSERT INTO equipment(name, type, model, motohours, status) VALUES(?,?,?,?,?)",
                name, type, model, 100.0 + i * 137.5, "READY"
            )
        }

        // --- водители ---
        val drivers = listOf("Иванов А.", "Петров С.", "Сидоров В.", "Кузнецов Д.")
        drivers.forEachIndexed { i, name ->
            exec("INSERT INTO drivers(name, equipment_id) VALUES(?,?)", name, i + 1)
        }

        // --- электролизёры: ряды, каждый делится пополам ---
        val numberToId = HashMap<Int, Int>()
        var number = 1
        for (r in 1..rows) {
            for (p in 1..perRow) {
                val half = if (p <= halfSize) "A" else "B"
                val series = if (p % 2 == 0) "С8БМ" else "С8БМ(Э)"
                val status = if (number % 37 == 0) "SERVICE" else "OK"
                val id = insertReturningId(
                    "INSERT INTO electrolyzers(number, building, row, position, half, series, status) VALUES(?,?,?,?,?,?,?)",
                    number, BUILDING, r, p, half, series, status
                )
                numberToId[number] = id
                number++
            }
        }

        // --- опасные зоны и тупики на схеме корпуса ---
        exec("INSERT INTO zones(kind,label,building,x,y,w,h) VALUES(?,?,?,?,?,?,?)", ZoneKind.DANGER, "Зона высокой температуры", BUILDING, 0.30, 0.02, 0.14, 0.16)
        exec("INSERT INTO zones(kind,label,building,x,y,w,h) VALUES(?,?,?,?,?,?,?)", ZoneKind.DANGER, "Проезд техники", BUILDING, 0.62, 0.42, 0.16, 0.14)
        exec("INSERT INTO zones(kind,label,building,x,y,w,h) VALUES(?,?,?,?,?,?,?)", ZoneKind.DEAD_END, "Тупик (разворот)", BUILDING, 0.01, 0.60, 0.12, 0.16)
        exec("INSERT INTO zones(kind,label,building,x,y,w,h) VALUES(?,?,?,?,?,?,?)", ZoneKind.DEAD_END, "Тупик у ворот 2", BUILDING, 0.86, 0.78, 0.12, 0.14)

        // --- ворота ---
        exec("INSERT INTO gates(name,building,x,y,is_open) VALUES(?,?,?,?,0)", "Ворота 1 (главные)", BUILDING, 0.18, 0.95)
        exec("INSERT INTO gates(name,building,x,y,is_open) VALUES(?,?,?,?,0)", "Ворота 2", BUILDING, 0.82, 0.95)

        // --- быстрые теги отклонений ---
        listOf(
            "Неисправность узла электролизёра",
            "Засор выгрузного лотка",
            "Нет глинозёма",
            "Повреждение корки",
            "Требуется ремонт",
            "Простой по погоде",
            "Прочее",
        ).forEach { exec("INSERT INTO deviation_tags(label) VALUES(?)", it) }

        // --- образцы заявок ---
        fun electrolyzerId(n: Int) = numberToId[n] ?: numberToId.values.first()

        // новая заявка на подсыпку (ждёт водителя, МРГ)
        exec(
            "INSERT INTO orders(type,status,description,electrolyzer_id,requested_by,is_repair,created_at) VALUES(?,?,?,?,?,?,?)",
            OrderType.PODSYPKA, OrderStatus.NEW, "Подсыпка глинозёма", electrolyzerId(125), "Диспетчер", 0, now - 3 * 60_000
        )
        // заявка в работе, принята 12 минут назад → «светофор» оранжевый
        exec(
            "INSERT INTO orders(type,status,description,electrolyzer_id,driver_id,requested_by,is_repair,created_at,accepted_at) VALUES(?,?,?,?,?,?,?,?,?)",
            OrderType.PODSYPKA, OrderStatus.ACCEPTED, "Подсыпка глинозёма", electrolyzerId(42), 1, "Диспетчер", 0, now - 14 * 60_000, now - 12 * 60_000
        )
        // заявка на обработку корки в работе
        exec(
            "INSERT INTO orders(type,status,description,electrolyzer_id,driver_id,requested_by,is_repair,created_at,accepted_at) VALUES(?,?,?,?,?,?,?,?,?)",
            OrderType.OBRABOTKA, OrderStatus.ACCEPTED, "Обработка корки", electrolyzerId(77), 3, "Диспетчер", 0, now - 40 * 60_000, now - 7 * 60_000
        )
        // завершённая заявка на ремонт (из кабины)
        exec(
            "INSERT INTO orders(type,status,description,electrolyzer_id,driver_id,requested_by,deviation_tag_id,comment,is_repair,motohours_delta,created_at,accepted_at,completed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
            OrderType.REMONT, OrderStatus.DONE, "Ремонт выгрузного лотка", electrolyzerId(9), 2, "Водитель Петров С.", 1, "Лоток засорён", 1, 1.5,
            now - 120 * 60_000, now - 110 * 60_000, now - 95 * 60_000
        )
    }

    // --------------------------------------------------------------- helpers

    private fun exec(sql: String, vararg args: Any?) {
        conn.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeUpdate()
        }
    }

    private fun insertReturningId(sql: String, vararg args: Any?): Int {
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> return if (rs.next()) rs.getInt(1) else 0 }
        }
    }

    private fun <T> query(sql: String, vararg args: Any?, map: (ResultSet) -> T): List<T> {
        conn.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeQuery().use { rs ->
                val out = ArrayList<T>()
                while (rs.next()) out.add(map(rs))
                return out
            }
        }
    }

    private fun one(sql: String, vararg args: Any?, map: (ResultSet) -> Unit): Boolean {
        conn.prepareStatement(sql).use { ps ->
            args.forEachIndexed { i, a -> ps.setObject(i + 1, a) }
            ps.executeQuery().use { rs -> if (rs.next()) { map(rs); return true } }
        }
        return false
    }

    // -------------------------------------------------------------- queries

    fun equipment(): List<Equipment> = query("SELECT * FROM equipment ORDER BY id") {
        Equipment(
            id = it.getInt("id"),
            name = it.getString("name"),
            type = it.getString("type"),
            model = it.getString("model"),
            motohours = it.getDouble("motohours"),
            status = it.getString("status"),
        )
    }

    fun equipmentById(id: Int): Equipment? {
        var res: Equipment? = null
        one("SELECT * FROM equipment WHERE id=?", id) {
            res = Equipment(it.getInt("id"), it.getString("name"), it.getString("type"), it.getString("model"), it.getDouble("motohours"), it.getString("status"))
        }
        return res
    }

    fun addMotohours(id: Int, delta: Double): Boolean {
        exec("UPDATE equipment SET motohours = motohours + ? WHERE id=?", delta, id)
        return equipmentById(id) != null
    }

    fun drivers(): List<Driver> = query(
        """
        SELECT d.id, d.name, d.equipment_id, e.name AS e_name, e.type AS e_type
        FROM drivers d LEFT JOIN equipment e ON d.equipment_id = e.id
        ORDER BY d.id
        """.trimIndent()
    ) {
        Driver(
            id = it.getInt("id"),
            name = it.getString("name"),
            equipmentId = it.getInt("equipment_id").takeIf { v -> !it.wasNull() },
            equipmentName = it.getString("e_name"),
            equipmentType = it.getString("e_type"),
        )
    }

    fun electrolyzers(building: String? = null): List<Electrolyzer> {
        val sql = "SELECT * FROM electrolyzers" + (building?.let { " WHERE building=?" } ?: "") + " ORDER BY row, position"
        return query(sql, *(building?.let { arrayOf<Any?>(it) } ?: emptyArray())) { mapElectrolyzer(it) }
    }

    private fun mapElectrolyzer(it: ResultSet) = Electrolyzer(
        id = it.getInt("id"),
        number = it.getInt("number"),
        building = it.getString("building"),
        row = it.getInt("row"),
        position = it.getInt("position"),
        half = it.getString("half"),
        series = it.getString("series"),
        status = it.getString("status"),
    )

    fun electrolyzerById(id: Int): Electrolyzer? {
        var res: Electrolyzer? = null
        one("SELECT * FROM electrolyzers WHERE id=?", id) { res = mapElectrolyzer(it) }
        return res
    }

    fun scheme(building: String = BUILDING): Scheme = Scheme(
        building = building,
        rows = rows,
        perRow = perRow,
        halfSize = halfSize,
        electrolyzerCount = electrolyzerCount,
        electrolyzers = electrolyzers(building),
        zones = query("SELECT * FROM zones WHERE building=? ORDER BY id", building) {
            Zone(it.getInt("id"), it.getString("kind"), it.getString("label"), it.getString("building"), it.getDouble("x"), it.getDouble("y"), it.getDouble("w"), it.getDouble("h"))
        },
        gates = query("SELECT * FROM gates WHERE building=? ORDER BY id", building) {
            Gate(it.getInt("id"), it.getString("name"), it.getString("building"), it.getDouble("x"), it.getDouble("y"), it.getInt("is_open") != 0)
        },
    )

    fun gates(): List<Gate> = query("SELECT * FROM gates ORDER BY id") {
        Gate(it.getInt("id"), it.getString("name"), it.getString("building"), it.getDouble("x"), it.getDouble("y"), it.getInt("is_open") != 0)
    }

    fun setGateOpen(id: Int, open: Boolean): Gate? {
        exec("UPDATE gates SET is_open=? WHERE id=?", if (open) 1 else 0, id)
        var res: Gate? = null
        one("SELECT * FROM gates WHERE id=?", id) {
            res = Gate(it.getInt("id"), it.getString("name"), it.getString("building"), it.getDouble("x"), it.getDouble("y"), it.getInt("is_open") != 0)
        }
        return res
    }

    fun tags(): List<DeviationTag> = query("SELECT * FROM deviation_tags ORDER BY id") {
        DeviationTag(it.getInt("id"), it.getString("label"))
    }

    fun orders(status: String? = null, driverId: Int? = null, isRepair: Boolean? = null): List<Order> {
        val conds = ArrayList<String>()
        val args = ArrayList<Any?>()
        status?.let { conds += "o.status = ?"; args += it }
        driverId?.let { conds += "o.driver_id = ?"; args += it }
        isRepair?.let { conds += "o.is_repair = ?"; args += if (it) 1 else 0 }
        val where = if (conds.isEmpty()) "" else " WHERE " + conds.joinToString(" AND ")
        val sql =
            """
            SELECT o.*, e.number AS e_number, e.building AS e_building, e.row AS e_row, e.position AS e_position, e.half AS e_half,
                   d.name AS d_name, t.label AS t_label
            FROM orders o
            LEFT JOIN electrolyzers e ON o.electrolyzer_id = e.id
            LEFT JOIN drivers d ON o.driver_id = d.id
            LEFT JOIN deviation_tags t ON o.deviation_tag_id = t.id
            $where
            ORDER BY CASE o.status WHEN 'NEW' THEN 0 WHEN 'ACCEPTED' THEN 1 ELSE 2 END, o.created_at DESC
            """.trimIndent()
        return query(sql, *args.toTypedArray()) { mapOrder(it) }
    }

    fun orderById(id: Int): Order? {
        var res: Order? = null
        one(
            """
            SELECT o.*, e.number AS e_number, e.building AS e_building, e.row AS e_row, e.position AS e_position, e.half AS e_half,
                   d.name AS d_name, t.label AS t_label
            FROM orders o
            LEFT JOIN electrolyzers e ON o.electrolyzer_id = e.id
            LEFT JOIN drivers d ON o.driver_id = d.id
            LEFT JOIN deviation_tags t ON o.deviation_tag_id = t.id
            WHERE o.id = ?
            """.trimIndent(), id
        ) { res = mapOrder(it) }
        return res
    }

    private fun mapOrder(it: ResultSet): Order {
        val status = it.getString("status")
        val type = it.getString("type")
        val acceptedAt = it.getLong("accepted_at").takeIf { v -> !it.wasNull() }
        val now = System.currentTimeMillis()
        val elapsed = UrgencyRules.elapsedMinutes(acceptedAt, now)
        val urgency = UrgencyRules.of(type, status, acceptedAt, now)
        return Order(
            id = it.getInt("id"),
            type = type,
            status = status,
            description = it.getString("description") ?: "",
            electrolyzerId = it.getInt("electrolyzer_id").takeIf { v -> !it.wasNull() },
            electrolyzerNumber = it.getInt("e_number").takeIf { v -> !it.wasNull() },
            building = it.getString("e_building"),
            row = it.getInt("e_row").takeIf { v -> !it.wasNull() },
            position = it.getInt("e_position").takeIf { v -> !it.wasNull() },
            half = it.getString("e_half"),
            driverId = it.getInt("driver_id").takeIf { v -> !it.wasNull() },
            driverName = it.getString("d_name"),
            requestedBy = it.getString("requested_by"),
            deviationTagId = it.getInt("deviation_tag_id").takeIf { v -> !it.wasNull() },
            deviationTagLabel = it.getString("t_label"),
            comment = it.getString("comment"),
            isRepair = it.getInt("is_repair") != 0,
            motohoursDelta = it.getDouble("motohours_delta").takeIf { v -> !it.wasNull() },
            createdAt = iso(it.getLong("created_at")),
            createdAtMillis = it.getLong("created_at"),
            acceptedAt = acceptedAt?.let { a -> iso(a) },
            acceptedAtMillis = acceptedAt,
            completedAt = it.getLong("completed_at").takeIf { v -> !it.wasNull() }?.let { c -> iso(c) },
            elapsedMinutes = elapsed,
            urgency = urgency,
        )
    }

    fun createOrder(type: String, electrolyzerId: Int?, description: String, requestedBy: String?, isRepair: Boolean): Order {
        val now = System.currentTimeMillis()
        val id = insertReturningId(
            "INSERT INTO orders(type,status,description,electrolyzer_id,requested_by,is_repair,created_at) VALUES(?,?,?,?,?,?,?)",
            type, OrderStatus.NEW, description, electrolyzerId, requestedBy, if (isRepair) 1 else 0, now
        )
        return orderById(id)!!
    }

    fun acceptOrder(orderId: Int, driverId: Int): Order? {
        val o = orderById(orderId) ?: return null
        if (o.status != OrderStatus.NEW) return o
        exec(
            "UPDATE orders SET status=?, driver_id=?, accepted_at=? WHERE id=?",
            OrderStatus.ACCEPTED, driverId, System.currentTimeMillis(), orderId
        )
        return orderById(orderId)
    }

    fun completeOrder(orderId: Int, deviationTagId: Int?, comment: String?, motohoursDelta: Double?): Order? {
        val o = orderById(orderId) ?: return null
        if (o.status == OrderStatus.DONE || o.status == OrderStatus.CANCELLED) return o
        val now = System.currentTimeMillis()
        exec(
            "UPDATE orders SET status=?, completed_at=?, deviation_tag_id=?, comment=?, motohours_delta=? WHERE id=?",
            OrderStatus.DONE, now, deviationTagId, comment, motohoursDelta, orderId
        )
        // начисляем моточасы на технику исполнителя
        val driverId = o.driverId
        if (driverId != null && motohoursDelta != null && motohoursDelta > 0) {
            val eq = drivers().firstOrNull { it.id == driverId }?.equipmentId
            if (eq != null) addMotohours(eq, motohoursDelta)
        }
        return orderById(orderId)
    }

    fun stats(): Stats {
        val equip = equipment()
        val all = orders()
        val driversById = drivers().associateBy { it.id }
        val rowsList = equip.map { e ->
            val done = all.filter { o ->
                o.status == OrderStatus.DONE && o.driverId != null && driversById[o.driverId]?.equipmentId == e.id
            }
            val durations = done.mapNotNull { o ->
                val a = o.acceptedAtMillis; val c = o.completedAt?.let { parseIso(it) }
                if (a != null && c != null) (c - a) / 60_000.0 else null
            }
            StatsRow(e, done.size, durations.takeIf { it.isNotEmpty() }?.average())
        }
        val byType = equip.groupBy { it.type }.mapValues { (_, list) -> list.sumOf { it.motohours } }
        return Stats(
            totalMotohours = equip.sumOf { it.motohours },
            byType = byType,
            rows = rowsList,
            ordersTotal = all.size,
            ordersNew = all.count { it.status == OrderStatus.NEW },
            ordersAccepted = all.count { it.status == OrderStatus.ACCEPTED },
            ordersDone = all.count { it.status == OrderStatus.DONE },
        )
    }

    fun config(): Map<String, Any> = mapOf(
        "building" to BUILDING,
        "rows" to rows,
        "perRow" to perRow,
        "halfSize" to halfSize,
        "electrolyzerCount" to electrolyzerCount,
    )

    private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()
    private fun parseIso(s: String): Long = try { Instant.parse(s).toEpochMilli() } catch (e: Exception) { 0L }
}
