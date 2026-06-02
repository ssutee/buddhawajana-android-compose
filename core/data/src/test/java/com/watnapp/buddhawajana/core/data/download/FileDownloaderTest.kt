package com.watnapp.buddhawajana.core.data.download

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.InputStream

class FileDownloaderTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `downloads bytes to dest and emits Progress then Done`() = runTest {
        val payload = ByteArray(2048) { it.toByte() }
        val downloader = FileDownloader(
            openStream = { _: String -> payload.size.toLong() to (ByteArrayInputStream(payload) as InputStream) },
        )
        val dest = tmp.newFile("out.pdf")
        var sawProgress = false
        var sawDone = false
        downloader.download("http://x/y.pdf", dest).test {
            while (true) {
                val e = awaitItem()
                if (e is DownloadProgress.Progress) sawProgress = true
                if (e is DownloadProgress.Done) { sawDone = true; break }
            }
            awaitComplete()
        }
        assertTrue(sawProgress); assertTrue(sawDone)
        assertArrayEquals(payload, dest.readBytes())
    }
}
