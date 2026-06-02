package com.watnapp.buddhawajana.core.network.dto

import com.squareup.moshi.Json

data class BookDto(
    val id: String,
    val name: String?,
    // String to match the API's wire contract (legacy ebookshop emits these as
    // JSON strings; iOS decodes them as String too). Parsed to Int in Mappers.
    @Json(name = "sort_order") val sortOrder: String?,
    val totalpage: String?,
    val producer: String?,
    val file: String?,
    val cover: String?,
    val category: String?,
)
