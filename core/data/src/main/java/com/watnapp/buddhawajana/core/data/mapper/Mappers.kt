package com.watnapp.buddhawajana.core.data.mapper

import com.watnapp.buddhawajana.core.data.db.AlbumEntity
import com.watnapp.buddhawajana.core.data.db.AudioEntity
import com.watnapp.buddhawajana.core.data.db.BookEntity
import com.watnapp.buddhawajana.core.data.db.BookmarkEntity
import com.watnapp.buddhawajana.core.data.db.ReadingProgressEntity
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.model.Bookmark
import com.watnapp.buddhawajana.core.model.ReadingProgress
import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import com.watnapp.buddhawajana.core.network.dto.BookDto

// ---- Book ----

fun BookDto.toEntity(): BookEntity = BookEntity(
    bookId = id.toLong(),
    title = name ?: "",
    coverUrl = cover ?: "",
    bookUrl = file ?: "",
    position = sortOrder ?: 0,
    pages = totalpage ?: 0,
    producer = producer ?: "",
    category = category ?: "",
    // remaining columns keep entity defaults
    progress = 0,
    status = 0,
    new = true,
    updated = false,
    detail = "",
    version = "",
    requestId = "",
)

fun BookEntity.toModel(): Book = Book(
    id = bookId.toString(),
    title = title,
    coverUrl = coverUrl.ifEmpty { null },
    fileUrl = bookUrl.ifEmpty { null },
    totalPage = pages,
    producer = producer.ifEmpty { null },
    category = category.ifEmpty { null },
    orderNumber = position,
)

// ---- Album ----

fun AlbumDto.toEntity(): AlbumEntity = AlbumEntity(
    albumId = id.toLong(),
    title = albumName ?: "",
    coverUrl = albumCover ?: "",
    itemCount = count ?: 0,
    position = 0,
    viewCount = 0,
)

fun AlbumEntity.toModel(): Album = Album(
    id = albumId.toString(),
    title = title,
    coverUrl = coverUrl.ifEmpty { null },
    itemCount = itemCount,
    position = position,
)

// ---- Audio ----

fun AudioDto.toEntity(albumId: String): AudioEntity = AudioEntity(
    audioId = id.toLong(),
    albumId = albumId.toLong(),
    title = name ?: "",
    url = fileUrl ?: "",
    // remaining columns keep entity defaults
    status = 0,
    requestId = 0,
    progress = 0,
)

fun AudioEntity.toModel(): Audio = Audio(
    id = audioId.toString(),
    albumId = albumId.toString(),
    title = title,
    url = url,
)

// ---- Bookmark ----

fun BookmarkEntity.toModel() = Bookmark(id, bookId, page, note, addedAt)

// ---- ReadingProgress ----

fun ReadingProgressEntity.toModel() = ReadingProgress(bookId, page, updatedAt)
