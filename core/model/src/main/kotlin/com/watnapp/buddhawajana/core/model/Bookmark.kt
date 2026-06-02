package com.watnapp.buddhawajana.core.model

data class Bookmark(
    val id: Long,
    val bookId: Long,
    val page: Int,
    val note: String,
    val addedAt: Long,
)
