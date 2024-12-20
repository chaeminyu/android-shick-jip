package com.example.shickjip.models

import org.w3c.dom.Comment
import java.util.UUID

data class Plant(
    val id: String = "", // Firestore document ID
    val userId: String = "", // User who added this plant
    val name: String = "", // Plant name
    val description: String = "", // Plant description
    val imagePath: String = "", // Local path to the plant image
    val captureDate: Long = 0, // Timestamp when plant was captured
    val registrationDate: Long = System.currentTimeMillis(), // When added to collection
    val diaryEntries: List<DiaryEntry> = emptyList()
)

data class DiaryEntry(
    val id: String = "",
    val content: String = "",
    val imagePath: String? = null,
    val date: Long = System.currentTimeMillis(),
    val comments: MutableList<DiaryComment> = mutableListOf()  // DiaryComment로 변경
)