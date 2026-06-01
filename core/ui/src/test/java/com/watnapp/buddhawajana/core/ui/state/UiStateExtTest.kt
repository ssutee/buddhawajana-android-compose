package com.watnapp.buddhawajana.core.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateExtTest {
    @Test
    fun `non-empty list becomes Content`() {
        val state = listOf("a", "b").toListUiState()
        assertTrue(state is UiState.Content)
        assertEquals(listOf("a", "b"), (state as UiState.Content).data)
    }

    @Test
    fun `empty list becomes Empty`() {
        assertEquals(UiState.Empty, emptyList<String>().toListUiState())
    }
}
