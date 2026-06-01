package com.watnapp.buddhawajana.core.model

data class Album(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val itemCount: Int,
    val position: Int,
)
