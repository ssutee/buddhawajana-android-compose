package com.watnapp.buddhawajana.core.data.download

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataProberTest {
    @Test fun `returns duration and size from probes`() = runTest {
        val prober = MetadataProber(headSize = { 12_000_000L }, readDuration = { 323_000L })
        val (dur, size) = prober.probe("http://x/a.mp3")
        assertEquals(323_000L, dur)
        assertEquals(12_000_000L, size)
    }

    @Test fun `null when probes throw`() = runTest {
        val prober = MetadataProber(headSize = { error("network") }, readDuration = { error("decode") })
        val (dur, size) = prober.probe("http://x/a.mp3")
        assertNull(dur)
        assertNull(size)
    }
}
