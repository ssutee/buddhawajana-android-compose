package com.watnapp.buddhawajana.feature.audio.format

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatTest {
    @Test fun `formatTime under an hour is M colon SS`() {
        assertEquals("5:23", formatTime(323_000))
        assertEquals("0:05", formatTime(5_000))
        assertEquals("0:00", formatTime(-10))
    }

    @Test fun `formatTime over an hour is H colon MM colon SS`() =
        assertEquals("1:02:03", formatTime(3_723_000))

    @Test fun `formatRowMeta joins duration and size`() =
        assertEquals("5:23 · 12 MB", formatRowMeta(323_000, 12_000_000))

    @Test fun `formatRowMeta shows dash when nothing known`() =
        assertEquals("—", formatRowMeta(null, null))

    @Test fun `formatRowMeta tolerates partial metadata`() {
        assertEquals("5:23", formatRowMeta(323_000, null))
        assertEquals("12 MB", formatRowMeta(null, 12_000_000))
    }

    @Test fun `formatRowMeta shows less-than-1MB for sub-1MB files`() =
        assertEquals("< 1 MB", formatRowMeta(null, 500_000))
}
