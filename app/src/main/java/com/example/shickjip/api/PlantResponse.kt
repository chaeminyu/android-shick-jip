package com.example.shickjip.api

data class PlantResponse(
    val access_token: String,
    val result: PlantResult
)

data class PlantResult(
    val is_plant: IsPlant,
    val classification: Classification
)

data class IsPlant(
    val binary: Boolean,
    val probability: Double
)

data class Classification(
    val suggestions: List<PlantSuggestion>
)

data class PlantSuggestion(
    val id: String,
    val name: String,
    val probability: Double,
    val similar_images: List<String>?,
    val details: PlantDetails?
)

data class PlantDetails(
    val common_names: List<String>?,
    val description: Description?,
    val taxonomy: Taxonomy?
)

data class Description(
    val value: String,
    val citation: String?
)

data class Taxonomy(
    val genus: String?,
    val species: String?,
    val family: String?
)