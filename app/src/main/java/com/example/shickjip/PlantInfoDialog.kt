package com.example.shickjip

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.shickjip.databinding.ItemInfomodalBinding
import com.example.shickjip.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

class PlantInfoDialog(
    private val title: String,
    private val description: String,
    private val imageUri: Uri?,
    private val imagePath: String,
    private val onSuccess: () -> Unit // Success callback
) : DialogFragment() {

    private var _binding: ItemInfomodalBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val PLANT_IMAGE_FOLDER = "plant_images"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ItemInfomodalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // 다이얼로그 크기 설정
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // 다이얼로그 배경을 투명하게 설정
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        // 기존 코드
        binding.plantTitle.text = title

        val normalizedDescription = description.replace("\n", " ")

        val maxTotalLength = 400
        val titleLength = title.length
        val remainingLength = maxTotalLength - titleLength

        val truncatedDescription = if (normalizedDescription.length > remainingLength) {
            normalizedDescription.substring(0, remainingLength - 5) + "...더보기"
        } else {
            normalizedDescription
        }

        binding.plantDescription.text = truncatedDescription

        if (imageUri != null) {
            Glide.with(requireContext()).load(imageUri).into(binding.plantImage)
        } else if (imagePath.isNotEmpty()) {
            Glide.with(requireContext()).load(File(imagePath)).into(binding.plantImage)
        }

        binding.registerButton.setOnClickListener {
            registerPlant()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun registerPlant() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            handleError("이미지가 선택되지 않았습니다.")
            return
        }

        binding.registerButton.isEnabled = false

        try {
            val file = saveImageToInternalStorage(imageUri)
            saveToFirestore(currentUser.uid, file.absolutePath)
        } catch (e: Exception) {
            Log.e("ImageSave", "이미지 저장 실패", e)
            handleError("이미지 저장 실패: ${e.message}")
        }
    }

    private fun saveImageToInternalStorage(imageUri: Uri): File {
        val contextResolver = requireContext().contentResolver
        val inputStream = contextResolver.openInputStream(imageUri) ?: throw Exception("이미지 스트림을 열 수 없습니다.")
        val imageFile = File(requireContext().filesDir, "Plant_${System.currentTimeMillis()}.jpg")

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
                Toast.makeText(requireContext(), "도감에 등록되었습니다!", Toast.LENGTH_SHORT).show()
                onSuccess()
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Firestore 데이터 저장 실패", e)
                handleError("도감 등록 실패: ${e.message}")
                binding.registerButton.isEnabled = true
            }
    }

    private fun handleError(message: String) {
        binding.registerButton.isEnabled = true
        Toast.makeText(requireContext(), "$message. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}