package com.example.shickjip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.shickjip.databinding.FragmentDiaryWritingBinding
import com.example.shickjip.models.DiaryEntry
import com.example.shickjip.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class DiaryWritingFragment : Fragment() {
    private var _binding: FragmentDiaryWritingBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var plantId: String
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.diaryImage.setImageURI(it)
            binding.diaryImage.visibility = View.VISIBLE
            binding.removeImageButton.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            plantId = it.getString(ARG_PLANT_ID) ?: throw IllegalArgumentException("Plant ID required")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiaryWritingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            binding.backButton.setOnClickListener {
                // 식물 상세 페이지로 돌아가기
                parentFragmentManager.popBackStack()
            }

            addImageButton.setOnClickListener {
                getContent.launch("image/*")
            }

            removeImageButton.setOnClickListener {
                selectedImageUri = null
                diaryImage.setImageURI(null)
                diaryImage.visibility = View.GONE
                removeImageButton.visibility = View.GONE
            }

            saveButton.setOnClickListener {
                saveDiaryEntry()
            }

            diaryContent.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    textCount.text = "${s?.length ?: 0}/150"
                    saveButton.isEnabled = !s.isNullOrEmpty()
                }
            })
        }
    }

    private fun saveDiaryEntry() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }

        val content = binding.diaryContent.text.toString()
        if (content.isEmpty()) {
            Toast.makeText(context, "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        // 일기 엔트리 생성
        val diaryEntry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            content = content,
            imagePath = null,  // 일단 null로 설정
            date = System.currentTimeMillis()
        )

        // 현재 식물 문서 가져오기
        firestore.collection("plants")
            .document(plantId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val plant = documentSnapshot.toObject(Plant::class.java)
                val diaryEntries = plant?.diaryEntries?.toMutableList() ?: mutableListOf()

                // 이미지가 있는 경우 저장
                if (selectedImageUri != null) {
                    try {
                        val imageFile = saveImageToInternalStorage(selectedImageUri!!)
                        diaryEntry.imagePath = imageFile.absolutePath
                    } catch (e: Exception) {
                        Log.e("DiaryWriting", "이미지 저장 실패", e)
                    }
                }

                // 일기 추가
                diaryEntries.add(diaryEntry)

                // Firestore 업데이트
                firestore.collection("plants")
                    .document(plantId)
                    .update("diaryEntries", diaryEntries)
                    .addOnSuccessListener {
                        // 경험치 및 코인 업데이트
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val userRef = firestore.collection("users").document(currentUser.uid)
                            firestore.runTransaction { transaction ->
                                val snapshot = transaction.get(userRef)
                                val currentExperience = snapshot.getLong("experience") ?: 0
                                val currentCoins = snapshot.getLong("coins") ?: 0

                                // 경험치와 코인 증가
                                transaction.update(userRef, "experience", currentExperience + 10)
                                transaction.update(userRef, "coins", currentCoins + 5)
                            }.addOnSuccessListener {
                                Toast.makeText(context, "경험치 +10, 코인 +5 획득!", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener { e ->
                                Log.e("DiaryWriting", "경험치 및 코인 업데이트 실패", e)
                                Toast.makeText(context, "경험치/코인 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Toast.makeText(context, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isAdded && !isRemoving) {
                                parentFragmentManager.popBackStack()
                            }
                        }, 300)
                    }
                    .addOnFailureListener { e ->
                        binding.saveButton.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.saveButton.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "데이터 로드에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveImageToInternalStorage(uri: Uri): File {
        val contextResolver = requireContext().contentResolver
        val inputStream = contextResolver.openInputStream(uri)
            ?: throw Exception("이미지 스트림을 열 수 없습니다.")

        val imageFile = File(requireContext().filesDir, "Diary_${System.currentTimeMillis()}.jpg")

        imageFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return imageFile
    }

    private suspend fun uploadImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = requireContext().contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image file")

        val imageRef = storage.reference
            .child("diary_images")
            .child(plantId)
            .child("${System.currentTimeMillis()}_${UUID.randomUUID()}")

        imageRef.putStream(stream).await()
        return@withContext imageRef.downloadUrl.await().toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PLANT_ID = "plant_id"

        fun newInstance(plantId: String) = DiaryWritingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PLANT_ID, plantId)
            }
        }
    }
}