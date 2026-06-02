package com.watnapp.buddhawajana.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpsUrlTest {
    @Test fun `upgrades http to https`() =
        assertEquals("https://x.com/a.mp3", "http://x.com/a.mp3".toHttpsOrSelf())

    @Test fun `trims surrounding whitespace`() =
        assertEquals("https://x.com/a.mp3", "  http://x.com/a.mp3 ".toHttpsOrSelf())

    @Test fun `leaves https untouched`() =
        assertEquals("https://x.com/a.mp3", "https://x.com/a.mp3".toHttpsOrSelf())

    @Test fun `leaves non-http scheme untouched`() =
        assertEquals("file:///a.mp3", "file:///a.mp3".toHttpsOrSelf())
}
