package com.example.shickjip

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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
        val content = binding.diaryContent.text.toString()
        if (content.isEmpty()) return

        binding.saveButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val imageUrl = selectedImageUri?.let { uploadImage(it) }

                val diaryEntry = DiaryEntry(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    imagePath = imageUrl,
                    date = System.currentTimeMillis()
                )

                firestore.collection("plants").document(plantId)
                    .update("diaryEntries", FieldValue.arrayUnion(diaryEntry))
                    .await()

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                binding.saveButton.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "일기 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
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