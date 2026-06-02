package com.watnapp.buddhawajana.feature.audio.list

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.state.UiState
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val downloads: com.watnapp.buddhawajana.core.data.repo.DownloadRepository =
        io.mockk.mockk(relaxed = true)

    private fun controller(): PlaybackController = mockk(relaxed = true) {
        every { nowPlaying } returns MutableStateFlow(null)
    }

    @Test fun `content streamed and search filters`() = runTest {
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(listOf(
                Audio("1", "9", "ตอนหนึ่ง", "http://x/1.mp3"),
                Audio("2", "9", "ตอนสอง", "http://x/2.mp3"),
            ))
            coEvery { refresh("9") } returns Result.success(Unit)
        }
        val vm = AudioListViewModel("9", repo, controller(), downloads)
        vm.onSearch("สอง")
        vm.state.test {
            var last: UiState<List<Audio>>? = null
            repeat(2) { last = awaitItem() }
            assertTrue(last is UiState.Content && (last as UiState.Content).data.single().id == "2")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `onRowVisible probes metadata for unprobed rows`() = runTest {
        val audio = Audio("1", "9", "ตอนหนึ่ง", "http://x/1.mp3", durationMs = null, sizeBytes = null)
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(listOf(audio))
            coEvery { refresh("9") } returns Result.success(Unit)
            coEvery { ensureMetadata(any()) } returns Unit
        }
        val vm = AudioListViewModel("9", repo, controller(), downloads)
        vm.onRowVisible(audio)
        advanceUntilIdle()
        coVerify { repo.ensureMetadata(audio) }
    }

    @Test fun `onRowVisible dedups repeated probes for same audio`() = runTest {
        val audio = Audio("1", "9", "ตอนหนึ่ง", "http://x/1.mp3", durationMs = null, sizeBytes = null)
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(listOf(audio))
            coEvery { refresh("9") } returns Result.success(Unit)
            coEvery { ensureMetadata(any()) } returns Unit
        }
        val vm = AudioListViewModel("9", repo, controller(), downloads)
        vm.onRowVisible(audio)
        vm.onRowVisible(audio)
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.ensureMetadata(audio) }
    }

    @Test fun `downloadAll enqueues whole album`() = runTest {
        val repo: AudioRepository = mockk(relaxed = true) {
            every { stream("9") } returns flowOf(emptyList())
            coEvery { refresh("9") } returns Result.success(Unit)
        }
        val vm = AudioListViewModel("9", repo, controller(), downloads)
        val album = com.watnapp.buddhawajana.core.model.Album("9", "A", null, 0, 0)
        val list = listOf(com.watnapp.buddhawajana.core.model.Audio("1", "9", "T", "u"))
        vm.downloadAll(album, list)
        advanceUntilIdle()
        io.mockk.coVerify { downloads.enqueueAll(album, list) }
    }
}
