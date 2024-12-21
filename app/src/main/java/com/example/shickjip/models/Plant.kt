package com.example.shickjip.models

import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.w3c.dom.Comment
import java.util.UUID

data class Plant(
    var id: String = "", // Firestore document ID
    val userId: String = "", // User who added this plant
    val name: String = "", // Scientific name
    val commonName: String? = null, // Common name
    val description: String = "",
    val imagePath: String = "",
    val captureDate: Long = 0,
    val registrationDate: Long = System.currentTimeMillis(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
    var nickname: String = "" // 닉네임 속성 추가
) {
    // 닉네임 설정
    fun ensureNickname(index: Int) {
        if (nickname.isEmpty()) {
            nickname = "식물 $index"
        }
    }
    // 표시용 이름을 가져오는 함수
    fun getDisplayName(): String {
        return nickname.ifEmpty { commonName ?: name }
    }
}

data class DiaryEntry(
    val id: String = "",
    val content: String = "",
    var imagePath: String? = null,
    val date: Long = System.currentTimeMillis(),
    val comments: MutableList<DiaryComment> = mutableListOf(),  // DiaryComment로 변경
    val nickname: String = ""
)

class PlantViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    val plantList = MutableLiveData<List<Plant>>()

    // 새로운 식물 저장
    fun savePlant(newPlant: Plant, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        // Firestore에서 현재 저장된 식물 개수를 가져옴
        firestore.collection("plants")
            .whereEqualTo("userId", currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                val plantCount = documents.size() + 1
                if (newPlant.nickname.isEmpty()) {
                    newPlant.nickname = "식물 $plantCount" // 닉네임 자동 설정
                }

                // Firestore에 저장
                firestore.collection("plants")
                    .add(newPlant)
                    .addOnSuccessListener {
                        Log.d("savePlant", "Plant saved successfully with nickname: ${newPlant.nickname}")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("savePlant", "Error saving plant: ${exception.message}")
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("savePlant", "Error fetching plant count: ${exception.message}")
                onError(exception)
            }
    }

    // 저장된 식물 목록 불러오기
    fun fetchPlants() {
        firestore.collection("plants")
            .whereEqualTo("userId", currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                val plants = documents.map { document ->
                    document.toObject(Plant::class.java).apply {
                        id = document.id // Firestore Document ID 설정
                    }
                }
                plantList.value = plants
                Log.d("fetchPlants", "Fetched ${plants.size} plants.")
            }
            .addOnFailureListener { exception ->
                Log.e("fetchPlants", "Error fetching plants: ${exception.message}")
            }
    }
}