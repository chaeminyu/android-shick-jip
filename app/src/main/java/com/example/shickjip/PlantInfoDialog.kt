package com.example.shickjip

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.shickjip.databinding.ItemInfomodalBinding
import com.example.shickjip.models.Plant
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

class PlantInfoDialog(
    context: Context,
    private val title: String,
    private val description: String,
    private val imageUri: Uri?,
    private val imagePath: String,
    private val onSuccess: () -> Unit // Success callback으로 변경
) : Dialog(context) {

    private lateinit var binding: ItemInfomodalBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    companion object {
        private const val PLANT_IMAGE_FOLDER = "plant_images"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase App Check 초기화
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        binding = ItemInfomodalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다이얼로그 스타일 설정
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.apply {
            plantTitle.text = title
            plantDescription.text = description

            if (imageUri != null) {
                Glide.with(context)
                    .load(imageUri)
                    .into(plantImage)
            } else if (imagePath.isNotEmpty()) {
                Glide.with(context)
                    .load(File(imagePath))
                    .into(plantImage)
            }

            registerButton.setOnClickListener {
                registerPlant()
            }

            closeButton.setOnClickListener {
                dismiss()
            }
        }

        // 텍스트 설정
        binding.plantTitle.text = title
        binding.plantDescription.text = description

        // 버튼 설정
        binding.registerButton.setOnClickListener {
            registerPlant()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

    }
    private fun registerPlant() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            handleError("이미지가 선택되지 않았습니다.")
            return
        }

        binding.registerButton.isEnabled = false

        try {
            // 이미지를 내부 저장소에 저장
            val file = saveImageToInternalStorage(imageUri)
            saveToFirestore(currentUser.uid, file.absolutePath)
        } catch (e: Exception) {
            Log.e("ImageSave", "이미지 저장 실패", e)
            handleError("이미지 저장 실패: ${e.message}")
        }
    }

    private fun saveImageToInternalStorage(imageUri: Uri): File {
        val contextResolver = context.contentResolver
        val inputStream = contextResolver.openInputStream(imageUri) ?: throw Exception("이미지 스트림을 열 수 없습니다.")
        val imageFile = File(context.filesDir, "Plant_${System.currentTimeMillis()}.jpg")

        imageFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return imageFile
    }

    private fun saveToFirestore(userId: String, filePath: String) {
        val plant = Plant(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = title,
            description = description,
            imagePath = filePath,
            captureDate = System.currentTimeMillis(),
            registrationDate = System.currentTimeMillis()
        )

        firestore.collection("plants")
            .document(plant.id)
            .set(plant)
            .addOnSuccessListener {
                Toast.makeText(context, "도감에 등록되었습니다!", Toast.LENGTH_SHORT).show()
                onSuccess() // Success callback 호출
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Firestore 데이터 저장 실패", e)
                handleError("도감 등록 실패: ${e.message}")
                binding.registerButton.isEnabled = true
            }
    }



//    private fun uploadImage(plantId: String, userId: String) {
//        try {
//            val storageRef = storage.reference
//                .child("plant_images") // Root folder
//                .child("${userId}_${plantId}.jpg") // Simplified path
//
//            val contentResolver = context.contentResolver
//            imageUri?.let { uri ->
//                val stream = contentResolver.openInputStream(uri)
//                val bytes = stream?.readBytes()
//                if (bytes != null) {
//                    storageRef.putBytes(bytes)
//                        .addOnSuccessListener {
//                            handleSuccess()
//                        }
//                        .addOnFailureListener { e ->
//                            Log.e("Storage", "Upload failed", e)
//                            handleError("이미지 업로드 실패")
//                        }
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("Storage", "Error preparing upload", e)
//            handleError("업로드 준비 실패")
//        }
//    }

    private fun handleSuccess() {
        Toast.makeText(context, "도감에 등록되었습니다!", Toast.LENGTH_SHORT).show()
        onSuccess()
        dismiss()
    }


    private fun handleError(message: String) {
        binding.registerButton.isEnabled = true
        Toast.makeText(context, "$message. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
    }
}
