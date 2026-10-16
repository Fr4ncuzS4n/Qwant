package com.example.qwant.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Базовый адрес сервера (`server/`), берётся из [ServerConfig].
 *
 * Дефолт — `http://10.0.2.2:8080`: `10.0.2.2` это хост-машина из Android-эмулятора.
 * Приложение позволяет сменить адрес на экране выбора кабинета (порт/адрес
 * залипают в SharedPreferences и применяются без пересборки APK).
 */
val BASE_URL: String get() = ServerConfig.baseUrl

/** Клиент REST API сервера. Тонкая обёртка над Ktor: только запросы, без логики. */
object QwantApi {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }
    }

    @Serializable
    data class AcceptRequest(val driverId: Int)

    @Serializable
    data class CreateOrderRequest(
        val type: String,
        val electrolyzerId: Int,
        val description: String? = null,
        val requestedBy: String? = null,
        val isRepair: Boolean = false,
    )

    @Serializable
    data class CompleteRequest(
        val deviationTagId: Int? = null,
        val comment: String? = null,
        val motohoursDelta: Double? = null,
    )

    suspend fun health(): Health = client.get("$BASE_URL/api/health").body()

    suspend fun drivers(): List<Driver> = client.get("$BASE_URL/api/drivers").body()

    suspend fun equipment(): List<Equipment> = client.get("$BASE_URL/api/equipment").body()

    suspend fun scheme(building: String = "4"): Scheme =
        client.get("$BASE_URL/api/scheme") { parameter("building", building) }.body()

    suspend fun tags(): List<DeviationTag> = client.get("$BASE_URL/api/tags").body()

    suspend fun stats(): Stats = client.get("$BASE_URL/api/stats").body()

    suspend fun gates(): List<Gate> = client.get("$BASE_URL/api/gates").body()

    suspend fun openGate(id: Int): Gate = client.post("$BASE_URL/api/gates/$id/open").body()

    suspend fun closeGate(id: Int): Gate = client.post("$BASE_URL/api/gates/$id/close").body()

    suspend fun orders(status: String? = null, driverId: Int? = null): List<Order> =
        client.get("$BASE_URL/api/orders") {
            status?.let { parameter("status", it) }
            driverId?.let { parameter("driverId", it) }
        }.body()

    suspend fun order(id: Int): Order = client.get("$BASE_URL/api/orders/$id").body()

    /** «Взять в работу» — переход NEW -> ACCEPTED. */
    suspend fun acceptOrder(orderId: Int, driverId: Int): Order =
        client.post("$BASE_URL/api/orders/$orderId/accept") {
            contentType(ContentType.Application.Json)
            setBody(AcceptRequest(driverId))
        }.body()

    /** «Выполнено» — переход в DONE, при указании моточасов начисляет наработку технике. */
    suspend fun completeOrder(orderId: Int, request: CompleteRequest): Order =
        client.post("$BASE_URL/api/orders/$orderId/complete") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun createOrder(request: CreateOrderRequest): Order =
        client.post("$BASE_URL/api/orders") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
