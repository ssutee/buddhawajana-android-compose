package com.watnapp.buddhawajana.core.model

data class Book(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val fileUrl: String?,
    val totalPage: Int?,
    val producer: String?,
    val orderNumber: Int,
)
