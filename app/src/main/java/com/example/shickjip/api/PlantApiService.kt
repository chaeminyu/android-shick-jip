package com.example.shickjip.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface PlantApiService {
    @Multipart
    @POST("identification")
    suspend fun identifyPlant(
        @Header("Api-Key") apiKey: String,
        @Part image: MultipartBody.Part,
        @Part("latitude") latitude: Float? = null,
        @Part("longitude") longitude: Float? = null,
        @Part("similar_images") similarImages: Boolean = true,
        @Query("details") details: String = "common_names,description,taxonomy,rank"
    ): Response<PlantResponse>
}