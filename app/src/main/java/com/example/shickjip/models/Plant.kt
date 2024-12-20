package com.example.shickjip.models

import org.w3c.dom.Comment
import java.util.UUID

data class Plant(
    val id: String = "", // Firestore document ID
    val userId: String = "", // User who added this plant
    val name: String = "", // Scientific name
    val commonName: String? = null, // Common name
    val description: String = "",
    val imagePath: String = "",
    val captureDate: Long = 0,
    val registrationDate: Long = System.currentTimeMillis(),
    val diaryEntries: List<DiaryEntry> = emptyList()
) {
    // 표시용 이름을 가져오는 함수
    fun getDisplayName(): String {
        return commonName ?: name
    }
}

data class DiaryEntry(
    val id: String = "",
    val content: String = "",
    val imagePath: String? = null,
    val date: Long = System.currentTimeMillis(),
    val comments: MutableList<DiaryComment> = mutableListOf()  // DiaryComment로 변경
)