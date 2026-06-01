package com.watnapp.buddhawajana.core.common

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class RunCatchingTest {
    @Test fun `wraps normal result`() = runTest {
        val r = runCatchingCancellable { 42 }
        assertEquals(42, r.getOrNull())
    }
    @Test fun `wraps ordinary throwable as failure`() = runTest {
        val r = runCatchingCancellable { throw RuntimeException("boom") }
        assertTrue(r.isFailure)
    }
    @Test fun `rethrows CancellationException`() = runTest {
        var rethrown = false
        try {
            runCatchingCancellable { throw CancellationException("cancel") }
        } catch (c: CancellationException) {
            rethrown = true
        }
        assertTrue(rethrown)
    }
}
