package com.example.shickjip.models

data class UserProfile(
    val uid: String = "",
    var username: String = "",
    val email: String = "",
    var coins: Long = 0,
    var experience: Long = 0,
    var level: Int = 1,
    val registrationDate: Long = System.currentTimeMillis()
)