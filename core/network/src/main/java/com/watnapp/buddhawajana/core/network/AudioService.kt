package com.watnapp.buddhawajana.core.network

import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import retrofit2.http.GET
import retrofit2.http.Path

interface AudioService {
    @GET("category")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("category/{id}/")
    suspend fun getAudios(@Path("id") albumId: String): List<AudioDto>
}
