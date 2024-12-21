package com.example.shickjip.api.translate

import com.example.shickjip.BuildConfig
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface DeepLApiService {
    @Headers("Authorization: DeepL-Auth-Key ${BuildConfig.DEEPL_API_KEY}")
    @FormUrlEncoded
    @POST("v2/translate")
    fun translate(
        @Field("text") text: String,
        @Field("target_lang") targetLang: String
    ): Call<TranslationResponse>
}