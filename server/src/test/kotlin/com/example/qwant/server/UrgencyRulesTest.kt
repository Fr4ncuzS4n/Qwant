package com.example.qwant.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UrgencyRulesTest {

    private val t0 = 1_000_000_000_000L

    @Test
    fun `green immediately after accept`() {
        assertEquals(Urgency.GREEN, UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.ACCEPTED, t0, t0))
    }

    @Test
    fun `still green at 9 minutes`() {
        assertEquals(Urgency.GREEN, UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.ACCEPTED, t0, t0 + 9 * 60_000))
    }

    @Test
    fun `orange at 10 minutes`() {
        assertEquals(Urgency.ORANGE, UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.ACCEPTED, t0, t0 + 10 * 60_000))
    }

    @Test
    fun `orange at 14 minutes`() {
        assertEquals(Urgency.ORANGE, UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.ACCEPTED, t0, t0 + 14 * 60_000))
    }

    @Test
    fun `red at 15 minutes`() {
        assertEquals(Urgency.RED, UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.ACCEPTED, t0, t0 + 15 * 60_000))
    }

    @Test
    fun `red after 15 minutes`() {
        assertEquals(Urgency.RED, UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.ACCEPTED, t0, t0 + 60 * 60_000))
    }

    @Test
    fun `no traffic light for non-podsypka orders`() {
        assertNull(UrgencyRules.of(OrderType.OBRABOTKA, OrderStatus.ACCEPTED, t0, t0 + 20 * 60_000))
        assertNull(UrgencyRules.of(OrderType.REMONT, OrderStatus.ACCEPTED, t0, t0 + 20 * 60_000))
    }

    @Test
    fun `no traffic light when not accepted`() {
        assertNull(UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.NEW, null, t0))
        assertNull(UrgencyRules.of(OrderType.PODSYPKA, OrderStatus.DONE, t0, t0 + 20 * 60_000))
    }

    @Test
    fun `elapsed minutes computation`() {
        assertEquals(12L, UrgencyRules.elapsedMinutes(t0, t0 + 12 * 60_000 + 30_000))
        assertNull(UrgencyRules.elapsedMinutes(null, t0))
    }
}
