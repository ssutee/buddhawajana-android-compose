package com.watnapp.buddhawajana.core.model

data class Download(
    val audioId: String,
    val title: String,
    val url: String,
    val albumId: String,
    val albumTitle: String,
    val coverUrl: String?,
    val sizeBytes: Long,
    val completedAt: Long,
)
