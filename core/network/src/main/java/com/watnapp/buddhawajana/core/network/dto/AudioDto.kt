package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class AudioDto(
    val id: String,
    val name: String?,
    @Json(name = "file_url") val fileUrl: String?,
)
