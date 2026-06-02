package com.watnapp.buddhawajana.feature.audio.player

import com.watnapp.buddhawajana.core.player.NowPlaying
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class PlayerViewModelTest {
    private val controller: PlaybackController = mockk(relaxed = true) {
        io.mockk.every { nowPlaying } returns MutableStateFlow<NowPlaying?>(null)
        io.mockk.every { isPlaying } returns MutableStateFlow(false)
        io.mockk.every { positionMs } returns MutableStateFlow(0L)
        io.mockk.every { durationMs } returns MutableStateFlow(0L)
        io.mockk.every { speed } returns MutableStateFlow(1.0f)
        io.mockk.every { sleepTimer } returns MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    }

    @Test fun `skip forward and back use the 15s delta`() {
        val vm = PlayerViewModel(controller)
        vm.skipForward(); verify { controller.skip(SKIP_DELTA_MS) }
        vm.skipBack(); verify { controller.skip(-SKIP_DELTA_MS) }
    }

    @Test fun `playPause and seek forward to controller`() {
        val vm = PlayerViewModel(controller)
        vm.playPause(); verify { controller.playPause() }
        vm.seekTo(1234L); verify { controller.seekTo(1234L) }
    }
}
