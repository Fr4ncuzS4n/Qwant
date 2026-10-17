package com.example.qwant.screens

import com.example.qwant.data.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты разграничения доступа по ролям: водитель не попадает на экраны
 * диспетчера, диспетчер — на экран водителя, без входа — только экран входа.
 */
class RoutesTest {

    // ------------------------------------------------------ стартовый маршрут

    @Test
    fun `without session the app starts at the login screen`() {
        assertEquals(Routes.AUTH, Routes.startFor(null))
    }

    @Test
    fun `dispatcher starts at dispatcher cabinet`() {
        assertEquals(Routes.DISPATCHER, Routes.startFor(Role.DISPATCHER))
    }

    @Test
    fun `driver starts at driver cabinet`() {
        assertEquals(Routes.DRIVER, Routes.startFor(Role.DRIVER))
    }

    // -------------------------------------------------------------- доступ

    @Test
    fun `driver cannot open dispatcher screens`() {
        assertFalse(Routes.isAllowed(Role.DRIVER, Routes.DISPATCHER))
        assertFalse(Routes.isAllowed(Role.DRIVER, Routes.CREATE_ORDER))
    }

    @Test
    fun `dispatcher cannot open driver cabinet`() {
        assertFalse(Routes.isAllowed(Role.DISPATCHER, Routes.DRIVER))
    }

    @Test
    fun `each role can open its own cabinet`() {
        assertTrue(Routes.isAllowed(Role.DRIVER, Routes.DRIVER))
        assertTrue(Routes.isAllowed(Role.DISPATCHER, Routes.DISPATCHER))
        assertTrue(Routes.isAllowed(Role.DISPATCHER, Routes.CREATE_ORDER))
    }

    @Test
    fun `shared screens are open to both roles`() {
        listOf(Routes.SCHEME, Routes.GATES, Routes.STATS).forEach { route ->
            assertTrue(route, Routes.isAllowed(Role.DRIVER, route))
            assertTrue(route, Routes.isAllowed(Role.DISPATCHER, route))
        }
        assertTrue(Routes.isAllowed(Role.DRIVER, Routes.order(42)))
        assertTrue(Routes.isAllowed(Role.DISPATCHER, Routes.order(42)))
    }

    @Test
    fun `shared screens require a session`() {
        assertFalse(Routes.isAllowed(null, Routes.SCHEME))
        assertFalse(Routes.isAllowed(null, Routes.STATS))
        assertFalse(Routes.isAllowed(null, Routes.DRIVER))
    }

    @Test
    fun `login screen is always reachable`() {
        assertTrue(Routes.isAllowed(null, Routes.AUTH))
        assertTrue(Routes.isAllowed(Role.DRIVER, Routes.AUTH))
        assertTrue(Routes.isAllowed(Role.DISPATCHER, Routes.AUTH))
    }

    @Test
    fun `developer screen is reachable without a session`() {
        assertTrue(Routes.isAllowed(null, Routes.DEVELOPER))
        assertTrue(Routes.isAllowed(Role.DRIVER, Routes.DEVELOPER))
        assertTrue(Routes.isAllowed(Role.DISPATCHER, Routes.DEVELOPER))
    }

    @Test
    fun `order route is built with the id`() {
        assertEquals("order/125", Routes.order(125))
    }
}
