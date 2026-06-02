package com.watnapp.buddhawajana.feature.audio.favorites

import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun fav(id: String) = Favorite(id, "T$id", "u$id", "9", "Album", "cov", id.toLong())

    @Test fun `empty favorites yields Empty state`() = runTest {
        val repo: FavoriteRepository = mockk(relaxed = true) { every { favorites } returns flowOf(emptyList()) }
        val vm = FavoritesViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Empty)
    }

    @Test fun `content then play builds single-track queue`() = runTest {
        val repo: FavoriteRepository = mockk(relaxed = true) { every { favorites } returns flowOf(listOf(fav("7"))) }
        val controller: PlaybackController = mockk(relaxed = true)
        val vm = FavoritesViewModel(repo, controller)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Content)
        vm.play(fav("7"))
        verify {
            controller.setQueue(
                Album("9", "Album", "cov", 0, 0),
                listOf(Audio("7", "9", "T7", "u7")),
                0,
            )
        }
    }

    @Test fun `remove forwards to repo`() = runTest {
        val repo: FavoriteRepository = mockk(relaxed = true) { every { favorites } returns flowOf(emptyList()) }
        val vm = FavoritesViewModel(repo, mockk(relaxed = true))
        vm.remove("7")
        advanceUntilIdle()
        coVerify { repo.remove("7") }
    }
}
