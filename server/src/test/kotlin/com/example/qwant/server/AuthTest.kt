package com.example.qwant.server

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.gson.gson
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** HTTP-тесты авторизации и разграничения ролей (testApplication). */
class AuthTest {

    // ------------------------------------------------------------- логин

    @Test
    fun `dispatcher logs in with correct credentials`() = testApplication {
        application {
            module(Db())
        }
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"dispatcher","password":"1234"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"DISPATCHER\""))
        assertTrue(response.bodyAsText().contains("\"token\""))
    }

    @Test
    fun `driver logs in and gets linked equipment`() = testApplication {
        application {
            module(Db())
        }
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"ivanov","password":"1234"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"DRIVER\""))
        assertTrue(body.contains("\"driverId\""))
        assertTrue(body.contains("МРГ-1"))
    }

    @Test
    fun `wrong password is rejected with 401`() = testApplication {
        application {
            module(Db())
        }
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"dispatcher","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `unknown login is rejected with 401`() = testApplication {
        application {
            module(Db())
        }
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"nobody","password":"1234"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `empty credentials are rejected with 400`() = testApplication {
        application {
            module(Db())
        }
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"","password":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `two logins get different tokens`() = testApplication {
        val db = Db()
        val a = db.authenticate("dispatcher", "1234")!!
        val b = db.authenticate("dispatcher", "1234")!!
        assertNotEquals(a.token, b.token)
    }

    @Test
    fun `logout invalidates the token`() {
        val db = Db()
        val login = db.authenticate("ivanov", "1234")!!
        assertEquals("Иванов А.", db.userByToken(login.token)?.name)
        db.logout(login.token)
        assertEquals(null, db.userByToken(login.token))
    }

    // --------------------------------------------------- разграничение ролей

    @Test
    fun `driver cannot create an order (403)`() = testApplication {
        val db = Db()
        val token = db.authenticate("ivanov", "1234")!!.token
        application {
            module(db)
        }
        val response = client.post("/api/orders") {
            header("X-Auth-Token", token)
            contentType(ContentType.Application.Json)
            setBody("""{"type":"PODSYPKA","electrolyzerId":1}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `anonymous cannot create an order (403)`() = testApplication {
        application {
            module(Db())
        }
        val response = client.post("/api/orders") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"PODSYPKA","electrolyzerId":1}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `dispatcher creates an order (201)`() = testApplication {
        val db = Db()
        val token = db.authenticate("dispatcher", "1234")!!.token
        application {
            module(db)
        }
        val response = client.post("/api/orders") {
            header("X-Auth-Token", token)
            contentType(ContentType.Application.Json)
            setBody("""{"type":"PODSYPKA","electrolyzerId":1}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("NEW"))
    }

    @Test
    fun `dispatcher cannot accept an order (403)`() = testApplication {
        val db = Db()
        val token = db.authenticate("dispatcher", "1234")!!.token
        val orderId = db.orders(status = OrderStatus.NEW).first().id
        application {
            module(db)
        }
        val response = client.post("/api/orders/$orderId/accept") {
            header("X-Auth-Token", token)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `anonymous cannot accept an order (403)`() = testApplication {
        val db = Db()
        val orderId = db.orders(status = OrderStatus.NEW).first().id
        application {
            module(db)
        }
        val response = client.post("/api/orders/$orderId/accept") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `driver accepts an order and becomes the executor`() = testApplication {
        val db = Db()
        val token = db.authenticate("ivanov", "1234")!!.token
        val orderId = db.orders(status = OrderStatus.NEW).first().id
        application {
            module(db)
        }
        val response = client.post("/api/orders/$orderId/accept") {
            header("X-Auth-Token", token)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"driverId\""))
        assertEquals(OrderStatus.ACCEPTED, db.orderById(orderId)!!.status)
    }

    @Test
    fun `driver cannot complete an order of another driver (403)`() = testApplication {
        val db = Db()
        val kuznetsov = db.authenticate("kuznetsov", "1234")!!.token // driverId = 4
        // заявка, принятая Ивановым (driverId = 1)
        val accepted = db.orders(status = OrderStatus.ACCEPTED).first { it.driverId == 1 }
        application {
            module(db)
        }
        val response = client.post("/api/orders/${accepted.id}/complete") {
            header("X-Auth-Token", kuznetsov)
            contentType(ContentType.Application.Json)
            setBody("""{"motohoursDelta":1.0}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `gates require authorization`() = testApplication {
        val db = Db()
        application {
            module(db)
        }
        val gateId = db.gates().first().id
        val anonymous = client.post("/api/gates/$gateId/open")
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)

        val token = db.authenticate("ivanov", "1234")!!.token
        val authorized = client.post("/api/gates/$gateId/open") { header("X-Auth-Token", token) }
        assertEquals(HttpStatusCode.OK, authorized.status)
        assertTrue(db.gates().first().isOpen)
    }

    @Test
    fun `me endpoint returns current user`() = testApplication {
        val db = Db()
        val token = db.authenticate("petrov", "1234")!!.token
        application {
            module(db)
        }
        val response = client.get("/api/auth/me") { header("X-Auth-Token", token) }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Петров С."))
        assertTrue(response.bodyAsText().contains("\"DRIVER\""))

        val anonymous = client.get("/api/auth/me")
        assertEquals(HttpStatusCode.Unauthorized, anonymous.status)
    }
}
