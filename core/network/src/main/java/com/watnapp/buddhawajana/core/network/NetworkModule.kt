package com.watnapp.buddhawajana.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
    single {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        moshi to client
    }
    single<BookService> {
        val (moshi, client) = get<Pair<Moshi, OkHttpClient>>()
        Retrofit.Builder()
            .baseUrl("http://etipitaka.org/ebookshop/oauth/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BookService::class.java)
    }
    single<AudioService> {
        val (moshi, client) = get<Pair<Moshi, OkHttpClient>>()
        Retrofit.Builder()
            .baseUrl("http://watnapahpong.com/api/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AudioService::class.java)
    }
}
