package com.watnapp.buddhawajana.feature.audio.player

import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import com.watnapp.buddhawajana.core.ui.BaseViewModel

class PlayerViewModel(private val controller: PlaybackController) : BaseViewModel() {
    val nowPlaying = controller.nowPlaying
    val isPlaying = controller.isPlaying
    val positionMs = controller.positionMs
    val durationMs = controller.durationMs
    val speed = controller.speed
    val sleepTimer = controller.sleepTimer

    fun playPause() = controller.playPause()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun skipForward() = controller.skip(SKIP_DELTA_MS)
    fun skipBack() = controller.skip(-SKIP_DELTA_MS)
    fun next() = controller.next()
    fun prev() = controller.prev()
    fun setSpeed(rate: Float) = controller.setSpeed(rate)
    fun setSleepTimer(state: SleepTimerState) = controller.setSleepTimer(state)
}
