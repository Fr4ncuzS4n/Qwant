package com.example.qwant.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.Instant

/*
 * Сервер приложения «водители дизельной техники» (клиент-серверное решение из ТЗ).
 *
 * Запуск:  ./gradlew -p server run
 * Порт:    PORT (по умолчанию 8080), слушает 0.0.0.0
 * БД:      sqlite3, образная база в памяти (QWANT_DB=путь, чтобы сохранять в файл)
 */

// ------------------------------------------------------------- тела запросов

data class CreateOrderRequest(
    val type: String? = null,
    val electrolyzerId: Int? = null,
    val description: String? = null,
    val requestedBy: String? = null,
    val isRepair: Boolean = false,
)

data class AcceptRequest(val driverId: Int? = null)

data class CompleteRequest(
    val deviationTagId: Int? = null,
    val comment: String? = null,
    val motohoursDelta: Double? = null,
)

data class MotohoursRequest(val delta: Double = 0.0)

// ------------------------------------------------------------------- module

fun Application.module(db: Db) {
    install(ContentNegotiation) { gson { setPrettyPrinting() } }
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "unknown error")))
        }
    }

    routing {
        get("/api/health") {
            call.respond(mapOf("status" to "ok", "service" to "qwant-server", "time" to Instant.now().toString()))
        }

        get("/api/config") { call.respond(db.config()) }

        // --- техника и моточасы ---
        get("/api/equipment") { call.respond(db.equipment()) }
        get("/api/equipment/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val eq = id?.let { db.equipmentById(it) }
            if (eq == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "equipment not found"))
            else call.respond(eq)
        }
        post("/api/equipment/{id}/motohours") {
            val id = call.parameters["id"]?.toIntOrNull()
            val req = call.receive<MotohoursRequest>()
            if (id == null || db.equipmentById(id) == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "equipment not found"))
            } else {
                db.addMotohours(id, req.delta)
                call.respond(db.equipmentById(id)!!)
            }
        }

        get("/api/drivers") { call.respond(db.drivers()) }

        // --- электролизёры и схема корпуса ---
        get("/api/electrolyzers") {
            call.respond(db.electrolyzers(call.request.queryParameters["building"]))
        }
        get("/api/scheme") {
            call.respond(db.scheme(call.request.queryParameters["building"] ?: "4"))
        }

        // --- ворота (дистанционное открытие) ---
        get("/api/gates") { call.respond(db.gates()) }
        post("/api/gates/{id}/open") {
            val id = call.parameters["id"]?.toIntOrNull()
            val g = id?.let { db.setGateOpen(it, true) }
            if (g == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "gate not found")) else call.respond(g)
        }
        post("/api/gates/{id}/close") {
            val id = call.parameters["id"]?.toIntOrNull()
            val g = id?.let { db.setGateOpen(it, false) }
            if (g == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "gate not found")) else call.respond(g)
        }

        // --- быстрые теги отклонений ---
        get("/api/tags") { call.respond(db.tags()) }

        // --- статистика наработки ---
        get("/api/stats") { call.respond(db.stats()) }

        // --- заявки ---
        get("/api/orders") {
            val status = call.request.queryParameters["status"]
            val driverId = call.request.queryParameters["driverId"]?.toIntOrNull()
            val isRepair = call.request.queryParameters["isRepair"]?.toBooleanStrictOrNull()
            call.respond(db.orders(status, driverId, isRepair))
        }

        get("/api/orders/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val o = id?.let { db.orderById(it) }
            if (o == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "order not found")) else call.respond(o)
        }

        post("/api/orders") {
            val req = call.receive<CreateOrderRequest>()
            val type = req.type ?: OrderType.OBRABOTKA
            val order = db.createOrder(
                type = type,
                electrolyzerId = req.electrolyzerId,
                description = req.description ?: defaultDescription(type),
                requestedBy = req.requestedBy,
                isRepair = req.isRepair || type == OrderType.REMONT,
            )
            call.respond(HttpStatusCode.Created, order)
        }

        post("/api/orders/{id}/accept") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad id"))
            val req = call.receive<AcceptRequest>()
            val driverId = req.driverId ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "driverId required"))
            val o = db.acceptOrder(id, driverId) ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "order not found"))
            call.respond(o)
        }

        post("/api/orders/{id}/complete") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad id"))
            val req = call.receive<CompleteRequest>()
            val o = db.completeOrder(id, req.deviationTagId, req.comment, req.motohoursDelta)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "order not found"))
            call.respond(o)
        }
    }
}

private fun defaultDescription(type: String): String = when (type) {
    OrderType.PODSYPKA -> "Подсыпка глинозёма"
    OrderType.OBRABOTKA -> "Обработка корки"
    OrderType.REMONT -> "Заявка на ремонт"
    else -> "Заявка"
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val db = Db()
    println("========================================================")
    println(" Qwant server запущен")
    println(" БД: sqlite3 (${System.getenv("QWANT_DB") ?: "в памяти"})")
    println(" Электролизёров: ${db.scheme().electrolyzerCount}, рядов: ${db.scheme().rows}, корпус: ${db.scheme().building}")
    println(" Слушаю http://0.0.0.0:$port")
    println("========================================================")
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module(db) }.start(wait = true)
}
