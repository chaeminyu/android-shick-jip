package com.example.shickjip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
                // 뒤로가기 시 ViewPager를 다시 표시
                (requireActivity() as HomeActivity).binding.viewPager.visibility = View.VISIBLE

                // Fragment를 pop
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
        // 현재 로그인된 사용자 확인
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // 로그인되지 않은 경우
            Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            return
        }

        val content = binding.diaryContent.text.toString()
        if (content.isEmpty()) return

        binding.saveButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 이미지가 있다면 내부 저장소에 저장
                val imagePath = selectedImageUri?.let { uri ->
                    saveImageToInternalStorage(uri).absolutePath
                }

                // 일기 엔트리 생성
                val diaryEntry = DiaryEntry(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    imagePath = imagePath,
                    date = System.currentTimeMillis()
                )

                // Firestore에 일기 추가
                firestore.collection("plants").document(plantId)
                    .update("diaryEntries", FieldValue.arrayUnion(diaryEntry))
                    .await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e is FirebaseAuthException) {
                        // 인증 관련 에러 처리
                        Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                    } else {
                        // 기타 에러 처리
                        binding.saveButton.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "일기 저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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