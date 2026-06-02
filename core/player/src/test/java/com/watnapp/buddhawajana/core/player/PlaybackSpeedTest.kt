package com.watnapp.buddhawajana.core.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedTest {
    @Test fun `presets are the iOS set`() =
        assertEquals(listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f), PlaybackSpeed.PRESETS)

    @Test fun `clamp snaps to nearest preset`() {
        assertEquals(1.0f, PlaybackSpeed.clamp(0.9f))
        assertEquals(1.5f, PlaybackSpeed.clamp(1.6f))
        assertEquals(2.0f, PlaybackSpeed.clamp(5f))
    }
}
