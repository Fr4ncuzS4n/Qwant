package com.example.qwant.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
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

data class CreateOrderRequest(
    val type: String? = null,
    val electrolyzerId: Int? = null,
    val description: String? = null,
    val requestedBy: String? = null,
    val isRepair: Boolean = false,
)

data class CompleteRequest(
    val deviationTagId: Int? = null,
    val comment: String? = null,
    val motohoursDelta: Double? = null,
)

data class MotohoursRequest(val delta: Double = 0.0)

/** Заголовок авторизации: `X-Auth-Token: <token>` (токен выдаёт `POST /api/auth/login`). */
private const val AUTH_HEADER = "X-Auth-Token"

/** Пользователь из заголовка авторизации или null, если токен не передан/недействителен. */
private fun ApplicationCall.currentUser(db: Db): UserAccount? =
    request.headers[AUTH_HEADER]?.takeIf { it.isNotBlank() }?.let { db.userByToken(it) }


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

        // --- авторизация ---
        post("/api/auth/login") {
            val req = call.receive<LoginRequest>()
            val login = req.login?.trim().orEmpty()
            val password = req.password.orEmpty()
            if (login.isEmpty() || password.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "login and password required"))
                return@post
            }
            val response = db.authenticate(login, password)
            if (response == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
            } else {
                call.respond(response)
            }
        }
        post("/api/auth/logout") {
            val user = call.currentUser(db)
            val token = call.request.headers[AUTH_HEADER]
            if (user != null && token != null) db.logout(token)
            call.respond(mapOf("status" to "ok"))
        }
        get("/api/auth/me") {
            val user = call.currentUser(db)
            if (user == null) call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "not authorized"))
            else call.respond(user)
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
            val user = call.currentUser(db)
            if (user == null || user.role != Role.DISPATCHER) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "dispatcher role required"))
                return@post
            }
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

        get("/api/buildings") { call.respond(db.buildings()) }

        get("/api/electrolyzers") {
            call.respond(db.electrolyzers(call.request.queryParameters["building"]))
        }
        get("/api/scheme") {
            val building = call.request.queryParameters["building"] ?: Db.DEFAULT_BUILDING
            if (!db.buildingExists(building)) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "building not found"))
            } else {
                call.respond(db.scheme(building))
            }
        }

        get("/api/gates") { call.respond(db.gates()) }
        post("/api/gates/{id}/open") {
            val user = call.currentUser(db)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "not authorized"))
                return@post
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val g = id?.let { db.setGateOpen(it, true) }
            if (g == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "gate not found")) else call.respond(g)
        }
        post("/api/gates/{id}/close") {
            val user = call.currentUser(db)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "not authorized"))
                return@post
            }
            val id = call.parameters["id"]?.toIntOrNull()
            val g = id?.let { db.setGateOpen(it, false) }
            if (g == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "gate not found")) else call.respond(g)
        }

        get("/api/tags") { call.respond(db.tags()) }

        get("/api/stats") { call.respond(db.stats()) }

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
            // Постановка заявки — только диспетчер.
            val user = call.currentUser(db)
            if (user == null || user.role != Role.DISPATCHER) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "dispatcher role required"))
                return@post
            }
            val req = call.receive<CreateOrderRequest>()
            val type = req.type ?: OrderType.OBRABOTKA
            val order = db.createOrder(
                type = type,
                electrolyzerId = req.electrolyzerId,
                description = req.description ?: defaultDescription(type),
                requestedBy = req.requestedBy ?: user.name,
                isRepair = req.isRepair || type == OrderType.REMONT,
            )
            call.respond(HttpStatusCode.Created, order)
        }

        post("/api/orders/{id}/accept") {
            // «Взять в работу» — только водитель; исполнителем становится он сам.
            val user = call.currentUser(db)
            if (user == null || user.role != Role.DRIVER || user.driverId == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "driver role required"))
                return@post
            }
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad id"))
            val o = db.acceptOrder(id, user.driverId) ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "order not found"))
            call.respond(o)
        }

        post("/api/orders/{id}/complete") {
            // «Выполнено» — только водитель; моточасы начисляются его технике.
            val user = call.currentUser(db)
            if (user == null || user.role != Role.DRIVER || user.driverId == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "driver role required"))
                return@post
            }
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad id"))
            val order = db.orderById(id)
            if (order == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "order not found"))
                return@post
            }
            if (order.status != OrderStatus.NEW && order.driverId != user.driverId) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "order belongs to another driver"))
                return@post
            }
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
    println(" Корпус по умолчанию: ${db.scheme().building}, рядов: ${db.scheme().rows}")
    println(" Слушаю http://0.0.0.0:$port")
    println(" Корпуса: " + db.buildings().joinToString(", ") { "${it.id} (${it.electrolyzerCount} эл.)" })
    println("========================================================")
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module(db) }.start(wait = true)
}
