package com.example.shickjip

data class ThemeItem(
    val id: String,                   // 고유 ID 추가
    val title: String,
    val description: String,
    val imageResId: Int,
    val price: Int,
    var isPurchased: Boolean = false
)