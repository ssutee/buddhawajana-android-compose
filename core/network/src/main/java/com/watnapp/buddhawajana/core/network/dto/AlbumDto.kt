package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class AlbumDto(
    val id: String,
    @Json(name = "album_name") val albumName: String?,
    @Json(name = "album_cover") val albumCover: String?,
    // API returns count as a quoted string (e.g. "63240"); parsed to Int in the mapper.
    val count: String?,
)
