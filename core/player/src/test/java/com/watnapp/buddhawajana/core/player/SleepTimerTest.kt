package com.watnapp.buddhawajana.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    @Test fun `duration ticks down and clamps at zero`() {
        assertEquals(4_000L, tickRemaining(5_000L, 1_000L))
        assertEquals(0L, tickRemaining(500L, 1_000L))
    }

    @Test fun `expired only at or below zero`() {
        assertFalse(isExpired(1L))
        assertTrue(isExpired(0L))
    }
}
