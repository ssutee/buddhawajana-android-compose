package com.watnapp.buddhawajana.feature.audio.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.player.PlaybackSpeed
import com.watnapp.buddhawajana.core.player.SleepTimerState
import com.watnapp.buddhawajana.feature.audio.format.formatTime

@Composable
fun PlayerScreen(vm: PlayerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val now by vm.nowPlaying.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.positionMs.collectAsState()
    val duration by vm.durationMs.collectAsState()

    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "ย่อ") }
        }
        CachedAsyncImage(
            url = now?.artworkUrl,
            contentDescription = now?.title,
            modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now?.title ?: "", style = MaterialTheme.typography.titleLarge)
            Text(now?.album ?: "", style = MaterialTheme.typography.bodyMedium)
        }

        var dragging by remember { mutableStateOf(false) }
        var draft by remember { mutableStateOf(0f) }
        val shown = if (dragging) draft else position.toFloat()
        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = shown,
                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                onValueChange = { dragging = true; draft = it },
                onValueChangeFinished = { if (dragging) { vm.seekTo(draft.toLong()); dragging = false } },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(shown.toLong()))
                Text(formatTime(duration))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = vm::prev) { Icon(Icons.Default.SkipPrevious, "ก่อนหน้า") }
            IconButton(onClick = vm::skipBack) { Icon(Icons.Default.FastRewind, "ถอย 15 วิ") }
            IconButton(onClick = vm::playPause) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "เล่น/หยุด")
            }
            IconButton(onClick = vm::skipForward) { Icon(Icons.Default.FastForward, "เดิน 15 วิ") }
            IconButton(onClick = vm::next) { Icon(Icons.Default.SkipNext, "ถัดไป") }
        }

        SpeedAndSleepRow(vm)

        ActionRow(vm)
    }
}

@Composable
private fun ActionRow(vm: PlayerViewModel) {
    val isFavorite by vm.isFavorite.collectAsState()
    val now by vm.nowPlaying.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        IconButton(onClick = vm::toggleFavorite, enabled = now != null) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "รายการโปรด",
            )
        }
        // Download button goes here (deferred to the Downloads vertical).
    }
}

@Composable
private fun SpeedAndSleepRow(vm: PlayerViewModel) {
    val speed by vm.speed.collectAsState()
    val sleep by vm.sleepTimer.collectAsState()
    var speedOpen by remember { mutableStateOf(false) }
    var sleepOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box {
            TextButton(onClick = { speedOpen = true }) { Text(speedLabel(speed)) }
            DropdownMenu(expanded = speedOpen, onDismissRequest = { speedOpen = false }) {
                PlaybackSpeed.PRESETS.forEach { r ->
                    DropdownMenuItem(text = { Text(speedLabel(r)) }, onClick = { vm.setSpeed(r); speedOpen = false })
                }
            }
        }
        Box {
            val label = when (val s = sleep) {
                is SleepTimerState.Duration -> formatTime(s.remainingMs)
                SleepTimerState.EndOfTrack -> "จบตอน"
                SleepTimerState.Off -> "ตั้งเวลา"
            }
            TextButton(onClick = { sleepOpen = true }) { Text(label) }
            DropdownMenu(expanded = sleepOpen, onDismissRequest = { sleepOpen = false }) {
                listOf(15, 30, 45, 60).forEach { min ->
                    DropdownMenuItem(
                        text = { Text("$min นาที") },
                        onClick = { vm.setSleepTimer(SleepTimerState.Duration(min * 60_000L)); sleepOpen = false },
                    )
                }
                DropdownMenuItem(text = { Text("จบตอนนี้") }, onClick = { vm.setSleepTimer(SleepTimerState.EndOfTrack); sleepOpen = false })
                DropdownMenuItem(text = { Text("ปิด") }, onClick = { vm.setSleepTimer(SleepTimerState.Off); sleepOpen = false })
            }
        }
    }
}

private fun speedLabel(rate: Float): String =
    if (rate % 1f == 0f) "${rate.toInt()}x" else "${rate}x"
