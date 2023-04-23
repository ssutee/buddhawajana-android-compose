package com.touchsi.buddhawajana.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.arges.sepan.argmusicplayer.Models.ArgAudio
import com.arges.sepan.argmusicplayer.Models.ArgAudioList
import com.arges.sepan.argmusicplayer.PlayerViews.ArgPlayerFullScreenView
import com.touchsi.buddhawajana.R
import com.touchsi.buddhawajana.entity.getFile
import com.touchsi.buddhawajana.repository.AlbumRepository
import com.touchsi.buddhawajana.repository.AudioRepository
import com.touchsi.buddhawajana.vm.DownloadableViewModel.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*

class Mp3PlayerActivity : AppCompatActivity() {
    private val audioRepo: AudioRepository by inject()
    private val albumRepo: AlbumRepository by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mp3_player)
        val albumId = intent.getLongExtra("ALBUM_ID", 0L)
        val index = intent.getIntExtra("START_INDEX", 0)
        val player = findViewById<ArgPlayerFullScreenView>(R.id.argmusicplayer)
        val appName = resources.getString(R.string.app_name)
        player.continuePlaylistWhenError()
        player.setPlaylistRepeat(true)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (player.isPaused) {
                    player.stop()
                    finish()
                } else {
                    player.pause()
                }
            }
        })

        val playlist = ArgAudioList(true)

        CoroutineScope(Dispatchers.Default).launch {
            val album = albumRepo.get(albumId)
            launch(Dispatchers.Main) {
                supportActionBar?.title = album.title
            }
            val list = audioRepo.getSimpleItems(albumId)
            launch(Dispatchers.Main) {
                Collections.rotate(list, -index)
                list.forEach { audio ->
                    val file = audio.getFile(this@Mp3PlayerActivity)
                    val fileUri = FileProvider.getUriForFile(this@Mp3PlayerActivity, "com.touchsi.buddhawajana.provider", file)
                    if (audio.status == Status.FINISHED.value && file.exists()) {
                        playlist.add(ArgAudio.createFromFilePath(appName, audio.title, file.absolutePath))
                    } else {
                        playlist.add(ArgAudio.createFromURL(appName, audio.title, audio.url))
                    }
                }
                player.playPlaylist(playlist)
            }
        }
    }
}