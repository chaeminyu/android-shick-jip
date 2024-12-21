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
            backButton.setOnClickListener {
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

        // Coroutine 사용하여 Firestore와 Firebase Storage 처리
        lifecycleScope.launch {
            try {
                val imageUrl = if (selectedImageUri != null) uploadImageToFirebase(selectedImageUri!!) else null
                saveDiaryToFirestore(content, imageUrl)
                Toast.makeText(context, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()

                // 경험치 및 코인 업데이트
                updateUserRewards()

                // 페이지 닫기
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded && !isRemoving) {
                        parentFragmentManager.popBackStack()
                    }
                }, 300)
            } catch (e: Exception) {
                Log.e("DiaryWriting", "Error saving diary entry", e)
                Toast.makeText(context, "저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun uploadImageToFirebase(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image file")

        val imageRef = storage.reference
            .child("diary_images")
            .child(plantId)
            .child("${System.currentTimeMillis()}_${UUID.randomUUID()}")

        imageRef.putStream(inputStream).await()
        return@withContext imageRef.downloadUrl.await().toString()
    }

    private suspend fun saveDiaryToFirestore(content: String, imageUrl: String?) = withContext(Dispatchers.IO) {
        val diaryEntry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            content = content,
            imagePath = imageUrl,
            date = System.currentTimeMillis()
        )

        // 기존 데이터 로드
        val plantRef = firestore.collection("plants").document(plantId)
        val plantSnapshot = plantRef.get().await()
        val plant = plantSnapshot.toObject(Plant::class.java)

        val updatedDiaryEntries = plant?.diaryEntries?.toMutableList() ?: mutableListOf()
        updatedDiaryEntries.add(diaryEntry)

        // Firestore 업데이트
        plantRef.update("diaryEntries", updatedDiaryEntries).await()
    }

    private suspend fun updateUserRewards() = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return@withContext

        val userRef = firestore.collection("users").document(currentUser.uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentExperience = snapshot.getLong("experience") ?: 0
            val currentCoins = snapshot.getLong("coins") ?: 0

            transaction.update(userRef, "experience", currentExperience + 10)
            transaction.update(userRef, "coins", currentCoins + 5)
        }.await()
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