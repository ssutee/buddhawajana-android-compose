package com.watnapp.buddhawajana.feature.audio.albums

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.model.Album
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
class AlbumsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val favorites: com.watnapp.buddhawajana.core.data.repo.FavoriteRepository =
        io.mockk.mockk(relaxed = true) {
            io.mockk.every { favorites } returns kotlinx.coroutines.flow.flowOf(emptyList())
        }

    private fun repo(albums: List<Album>): AlbumRepository = mockk(relaxed = true) {
        every { stream() } returns flowOf(albums)
        coEvery { refresh() } returns Result.success(Unit)
    }

    @Test fun `emits content from stream`() = runTest {
        val vm = AlbumsViewModel(repo(listOf(Album("1", "ชุดA", null, 3, 0))), favorites)
        vm.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = awaitItem()
            assertTrue(content is UiState.Content && content.data.single().title == "ชุดA")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `search filters by title case-insensitively`() = runTest {
        val vm = AlbumsViewModel(repo(listOf(
            Album("1", "ตถาคต", null, 1, 0),
            Album("2", "ภิกษุ", null, 1, 0),
        )), favorites)
        vm.onSearch("ตถา")
        vm.state.test {
            var last: UiState<List<Album>>? = null
            repeat(2) { last = awaitItem() }
            assertTrue(last is UiState.Content && (last as UiState.Content).data.single().id == "1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `favoriteCount reflects repo`() = runTest {
        val favs: com.watnapp.buddhawajana.core.data.repo.FavoriteRepository =
            io.mockk.mockk(relaxed = true) {
                io.mockk.every { favorites } returns kotlinx.coroutines.flow.flowOf(
                    listOf(
                        com.watnapp.buddhawajana.core.model.Favorite("1", "T", "u", "9", "A", null, 1),
                        com.watnapp.buddhawajana.core.model.Favorite("2", "T", "u", "9", "A", null, 2),
                    )
                )
            }
        val vm = AlbumsViewModel(repo(emptyList()), favs)
        vm.favoriteCount.test {
            var last = 0
            repeat(2) { last = awaitItem() }
            org.junit.Assert.assertEquals(2, last)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
