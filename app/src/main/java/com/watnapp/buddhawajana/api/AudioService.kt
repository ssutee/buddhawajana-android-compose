package com.watnapp.buddhawajana.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.watnapp.buddhawajana.entity.AlbumEntity
import com.watnapp.buddhawajana.entity.AudioEntity
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.*

fun AlbumJson.toEntity(): AlbumEntity {
    val entity = AlbumEntity()
    entity.albumId = this.id
    entity.title = this.album_name
    entity.coverUrl = this.album_cover
    entity.viewCount = this.count
    entity.updatedAt = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK).parse(this.updated)!!
    return entity
}

data class AlbumJson(@field:Json(name = "id") var id: Long = 0) {
    @field:Json(name = "album_name") lateinit var album_name: String
    @field:Json(name = "album_cover") lateinit var album_cover: String
    @field:Json(name = "count") var count: Long = 0
    @field:Json(name = "created") lateinit var created: String
    @field:Json(name = "updated") lateinit var updated: String
}

fun AudioJson.toEntity(): AudioEntity {
    val entity = AudioEntity()
    entity.audioId = this.id
    entity.title = this.name
    entity.url = this.file_url
    entity.updatedAt = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK)
        .parse(if(this.updated != "") this.updated else this.created)!!
    return entity
}

data class AudioJson(@field:Json(name = "id") var id: Long = 0) {
    @field:Json(name = "file_url") lateinit var file_url: String
    @field:Json(name = "name") lateinit var name: String
    @field:Json(name = "updated") var updated: String = ""
    @field:Json(name = "created") lateinit var created: String
}

interface AudioService {
    @GET("category")
    fun getAllAlbums(): Observable<List<AlbumJson>>

    @GET("category/{albumId}/")
    fun getAllAudios(@Path("albumId") albumId: Long): Observable<List<AudioJson>>

    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val interceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        private val client : OkHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(interceptor)
        }.build()

        fun create(): AudioService {
            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl("http://watnapahpong.com/api/")
                .client(client)
                .build()
            return retrofit.create(AudioService::class.java)
        }
    }
}