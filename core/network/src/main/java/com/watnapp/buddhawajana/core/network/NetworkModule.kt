package com.watnapp.buddhawajana.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
    single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }
    single<BookService> {
        Retrofit.Builder()
            .baseUrl("http://etipitaka.org/ebookshop/oauth/")
            .client(get<OkHttpClient>())
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi>()))
            .build()
            .create(BookService::class.java)
    }
    single<AudioService> {
        Retrofit.Builder()
            .baseUrl("http://watnapahpong.com/api/")
            .client(get<OkHttpClient>())
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi>()))
            .build()
            .create(AudioService::class.java)
    }
}
