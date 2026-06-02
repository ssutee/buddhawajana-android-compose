package com.watnapp.buddhawajana.core.data.download

import android.content.Context
import android.os.Environment
import java.io.File

class AudioFileStore(private val context: Context) {
    private fun dir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "buddhawajana/audios").apply { mkdirs() }
    fun file(audioId: String): File = File(dir(), "$audioId.mp3")
    fun exists(audioId: String): Boolean = file(audioId).let { it.exists() && it.length() > 0 }
    fun delete(audioId: String): Boolean = file(audioId).delete()
}
