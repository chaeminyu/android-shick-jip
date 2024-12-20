package com.example.shickjip.models

data class UserProfile(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val level: Int = 1,
    val experience: Int = 0,
    val coins: Long = 0, // Long 타입으로 선언
    val registrationDate: Long = System.currentTimeMillis()
)