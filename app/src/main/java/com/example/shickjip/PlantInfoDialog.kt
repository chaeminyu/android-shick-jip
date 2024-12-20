package com.example.shickjip

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.example.shickjip.databinding.ItemInfomodalBinding
import com.example.shickjip.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class PlantInfoDialog(
    context: Context,
    private val title: String,
    private val description: String,
    private val imagePath: String,
    private val onButtonClick: () -> Unit // 버튼 클릭 콜백
) : Dialog(context) {

    private lateinit var binding: ItemInfomodalBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ItemInfomodalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다이얼로그 스타일 설정
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // 텍스트 설정
        binding.plantTitle.text = title
        binding.plantDescription.text = description

        // 버튼 설정
        binding.registerButton.setOnClickListener {
            onButtonClick() // 콜백 호출
            dismiss() // 다이얼로그 닫기
        }

        binding.closeButton.setOnClickListener {
            onButtonClick() // 콜백 호출
            dismiss() // 다이얼로그 닫기
        }
    }
    private fun registerPlant() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.registerButton.isEnabled = false

        // Upload image to Firebase Storage
        val imageFile = File(imagePath)
        val imageRef = storage.reference.child("plants/${currentUser.uid}/${System.currentTimeMillis()}_${imageFile.name}")

        imageRef.putFile(Uri.fromFile(imageFile))
            .addOnSuccessListener { taskSnapshot ->
                // Get download URL
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Create plant document
                    val plant = Plant(
                        userId = currentUser.uid,
                        name = title,
                        description = description,
                        imagePath = downloadUri.toString(),
                        captureDate = System.currentTimeMillis()
                    )

                    // Save to Firestore
                    firestore.collection("plants")
                        .add(plant)
                        .addOnSuccessListener {
                            Toast.makeText(context, "도감에 등록되었습니다!", Toast.LENGTH_SHORT).show()
                            onButtonClick()
                            dismiss()
                        }
                        .addOnFailureListener { e ->
                            Log.e("PlantInfoDialog", "Error adding plant", e)
                            Toast.makeText(context, "등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                            binding.registerButton.isEnabled = true
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PlantInfoDialog", "Error uploading image", e)
                Toast.makeText(context, "이미지 업로드에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
            }
    }
}