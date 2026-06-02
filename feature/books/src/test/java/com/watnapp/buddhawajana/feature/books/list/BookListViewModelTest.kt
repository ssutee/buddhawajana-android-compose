package com.watnapp.buddhawajana.feature.books.list

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coEvery
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModelTest {
    // NOTE: match the real Book(...) field order (id,title,coverUrl,fileUrl,totalPage,producer,category,orderNumber)
    private fun book(id: String, title: String) = Book(id, title, null, "u", 1, null, "cat", 0)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads books as Content and search filters by title`() = runTest {
        val repo = mockk<BookRepository>(relaxed = true)
        every { repo.stream() } returns flowOf(listOf(book("1", "Marga"), book("2", "Tathagata")))
        coEvery { repo.refresh() } returns Result.success(Unit)
        val vm = BookListViewModel(repo, downloaded = { false })

        vm.state.test {
            var s = awaitItem()
            while (s is UiState.Loading) s = awaitItem()
            assertTrue(s is UiState.Content)
            assertEquals(2, (s as UiState.Content).data.size)

            vm.onSearch("marga")
            val filtered = awaitItem()
            assertEquals(1, (filtered as UiState.Content).data.size)
            assertEquals("Marga", filtered.data[0].book.title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
