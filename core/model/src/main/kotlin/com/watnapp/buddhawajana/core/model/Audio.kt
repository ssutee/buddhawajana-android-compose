package com.watnapp.buddhawajana.core.model

data class Audio(
    val id: String,
    val albumId: String,
    val title: String,
    val url: String,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
)
