package com.watnapp.buddhawajana.feature.audio.downloads

import com.watnapp.buddhawajana.core.data.repo.DownloadRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Download
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
class DownloadsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun dl(id: String) = Download(id, "T$id", "u$id", "9", "Album", "cov", 1_000L, id.toLong())

    @Test fun `empty downloads yields Empty`() = runTest {
        val repo: DownloadRepository = mockk(relaxed = true) { every { downloads } returns flowOf(emptyList()) }
        val vm = DownloadsViewModel(repo, mockk(relaxed = true))
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Empty)
    }

    @Test fun `play builds single-track queue`() = runTest {
        val repo: DownloadRepository = mockk(relaxed = true) { every { downloads } returns flowOf(listOf(dl("7"))) }
        val controller: PlaybackController = mockk(relaxed = true)
        val vm = DownloadsViewModel(repo, controller)
        advanceUntilIdle()
        vm.play(dl("7"))
        verify { controller.setQueue(Album("9", "Album", "cov", 0, 0), listOf(Audio("7", "9", "T7", "u7")), 0) }
    }

    @Test fun `delete forwards`() = runTest {
        val repo: DownloadRepository = mockk(relaxed = true) { every { downloads } returns flowOf(emptyList()) }
        val vm = DownloadsViewModel(repo, mockk(relaxed = true))
        vm.delete("7")
        advanceUntilIdle()
        coVerify { repo.delete("7") }
    }
}
