package com.example.shickjip.api.translate

data class TranslationResponse(
    val translations: List<Translation>
)

data class Translation(
    val detected_source_language: String,
    val text: String
)