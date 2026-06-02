package com.watnapp.buddhawajana.feature.books.reader

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.core.data.download.DownloadProgress
import com.watnapp.buddhawajana.core.data.download.FileDownloader
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.model.Book
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `missing file downloads then reaches Ready`() = runTest {
        val store = mockk<BookFileStore>()
        val file = File.createTempFile("book", ".pdf")
        every { store.exists(1L) } returns false
        every { store.file(1L) } returns file
        val downloader = mockk<FileDownloader>()
        every { downloader.download(any(), any()) } returns flowOf(DownloadProgress.Progress(1, 2), DownloadProgress.Done)
        val books = mockk<BookRepository>(relaxed = true)
        every { books.stream() } returns flowOf(listOf(Book("1", "T", null, "http://x/y.pdf", 1, null, null, 0)))
        val vm = ReaderViewModel(
            bookId = 1L, books = books, downloader = downloader, files = store,
            bookmarks = mockk(relaxed = true), progress = mockk(relaxed = true),
            openPdf = { _ -> FakePdf() },
        )
        vm.state.test {
            var seenReady = false
            for (i in 0 until 8) {
                val s = awaitItem()
                if (s is ReaderState.Ready) { seenReady = true; break }
            }
            assertTrue(seenReady)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
