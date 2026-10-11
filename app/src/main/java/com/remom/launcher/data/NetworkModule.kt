package com.remom.launcher.data

import com.remom.launcher.data.modrinth.ModrinthApi
import com.remom.launcher.data.mojang.MojangApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val MODRINTH_BASE = "https://api.modrinth.com/v2/"
    private const val MOJANG_BASE = "https://piston-meta.mojang.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "RemomLauncher/0.1.0 (github.com/anjalifredy-ai/REMONLAUNCHER-JAVA)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(logging)
        .build()

    val modrinthApi: ModrinthApi by lazy {
        Retrofit.Builder()
            .baseUrl(MODRINTH_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModrinthApi::class.java)
    }

    val mojangApi: MojangApi by lazy {
        Retrofit.Builder()
            .baseUrl(MOJANG_BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MojangApi::class.java)
    }
}
