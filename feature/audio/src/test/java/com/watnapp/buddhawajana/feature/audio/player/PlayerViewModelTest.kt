package com.watnapp.buddhawajana.feature.audio.player

import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.NowPlaying
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val favorites: FavoriteRepository = mockk(relaxed = true) {
        every { isFavorite(any()) } returns flowOf(false)
    }
    private fun controller(now: NowPlaying? = null): PlaybackController = mockk(relaxed = true) {
        every { nowPlaying } returns MutableStateFlow(now)
        every { isPlaying } returns MutableStateFlow(false)
        every { positionMs } returns MutableStateFlow(0L)
        every { durationMs } returns MutableStateFlow(0L)
        every { speed } returns MutableStateFlow(1.0f)
        every { sleepTimer } returns MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    }

    @Test fun `skip forward and back use the 15s delta`() {
        val c = controller()
        val vm = PlayerViewModel(c, favorites)
        vm.skipForward(); coVerify { c.skip(SKIP_DELTA_MS) }
        vm.skipBack(); coVerify { c.skip(-SKIP_DELTA_MS) }
    }

    @Test fun `playPause and seek forward to controller`() {
        val c = controller()
        val vm = PlayerViewModel(c, favorites)
        vm.playPause(); coVerify { c.playPause() }
        vm.seekTo(1234L); coVerify { c.seekTo(1234L) }
    }

    @Test fun `toggleFavorite snapshots current track`() = runTest {
        val now = NowPlaying("7", "9", "Talk", "Album", "cov", "http://x/7.mp3", isLocal = false)
        val vm = PlayerViewModel(controller(now), favorites)
        vm.toggleFavorite()
        advanceUntilIdle()
        coVerify { favorites.toggle(match<Favorite> { it.audioId == "7" && it.url == "http://x/7.mp3" && it.albumTitle == "Album" }) }
    }

    @Test fun `toggleFavorite no-ops when nothing playing`() = runTest {
        val vm = PlayerViewModel(controller(null), favorites)
        vm.toggleFavorite()
        advanceUntilIdle()
        coVerify(exactly = 0) { favorites.toggle(any()) }
    }
}
