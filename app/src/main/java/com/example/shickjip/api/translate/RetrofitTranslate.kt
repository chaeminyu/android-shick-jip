package com.example.shickjip.api.translate

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitTranslate {
    private const val BASE_URL = "https://api-free.deepl.com/"

    val apiService: DeepLApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepLApiService::class.java)
    }
}