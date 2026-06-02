package com.watnapp.buddhawajana.core.model

data class PlaybackProgress(
    val audioId: String,
    val positionMs: Long,
    val updatedAt: Long,
)
