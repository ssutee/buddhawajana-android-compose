package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class BookDto(
    val id: String,
    val name: String?,
    @Json(name = "sort_order") val sortOrder: Int?,
    val totalpage: Int?,
    val producer: String?,
    val file: String?,
    val cover: String?,
)
