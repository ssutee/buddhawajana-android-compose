package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class AlbumDto(
    val id: String,
    @Json(name = "album_name") val albumName: String?,
    @Json(name = "album_cover") val albumCover: String?,
    val count: Int?,
)
