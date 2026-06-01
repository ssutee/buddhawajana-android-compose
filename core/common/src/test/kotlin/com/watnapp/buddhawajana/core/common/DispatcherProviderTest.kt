package com.watnapp.buddhawajana.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatcherProviderTest {
    @Test
    fun `default provider exposes standard dispatchers`() {
        val provider = DefaultDispatcherProvider()
        assertEquals(Dispatchers.IO, provider.io)
        assertEquals(Dispatchers.Default, provider.default)
        assertEquals(Dispatchers.Main, provider.main)
    }
}
