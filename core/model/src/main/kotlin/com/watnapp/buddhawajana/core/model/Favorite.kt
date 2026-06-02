package com.watnapp.buddhawajana.core.model

data class Favorite(
    val audioId: String,
    val title: String,
    val url: String,
    val albumId: String,
    val albumTitle: String,
    val coverUrl: String?,
    val addedAt: Long,
)
