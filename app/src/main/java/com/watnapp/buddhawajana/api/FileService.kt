package com.watnapp.buddhawajana.api

import android.os.SystemClock
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface FileService {
    @Streaming @GET
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody

    companion object {
        fun create(): FileService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val loggingInterceptor = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.NONE)
            val dispatcher = Dispatcher()
            dispatcher.maxRequests = 1
            val interceptor = Interceptor { chain ->
                SystemClock.sleep(1000)
                chain.proceed(chain.request())
            }
            return Retrofit.Builder()
                .client(
                    OkHttpClient.Builder()
                        .readTimeout(30L, TimeUnit.SECONDS)
                        .writeTimeout(30L, TimeUnit.SECONDS)
                        .addInterceptor(loggingInterceptor)
                        .addNetworkInterceptor(interceptor)
                        .dispatcher(dispatcher)
                        .build()
                )
                .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                .baseUrl("http://download.watnapahpong.org/")
                .build()
                .create(FileService::class.java)
        }
    }
}