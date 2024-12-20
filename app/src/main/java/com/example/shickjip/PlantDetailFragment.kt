package com.example.shickjip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.shickjip.databinding.FragmentPlantDetailBinding
import com.example.shickjip.models.DiaryComment
import com.example.shickjip.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDetailFragment : Fragment() {
    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    private var plant: Plant? = null
    private lateinit var plantId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            plantId = it.getString(ARG_PLANT_ID) ?: throw IllegalArgumentException("Plant ID required")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadPlantDetails()
    }

    private fun setupViews() {
        binding.backButton.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack() // Fragment를 BackStack에서 제거
        }

        binding.writeDiaryButton.setOnClickListener {
            // 현재 띄워져 있는 ViewPager를 감춤
            (requireActivity() as HomeActivity).binding.viewPager.visibility = View.GONE

            // DiaryWritingFragment로 전환
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DiaryWritingFragment.newInstance(plantId))
                .addToBackStack("diary_writing")  // 백스택에 이름을 지정하여 추가
                .commit()
        }
    }


    private fun loadPlantDetails() {
        firestore.collection("plants").document(plantId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PlantDetailFragment", "Error fetching plant details", e)
                    Toast.makeText(context, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(Plant::class.java)?.let { plant ->
                        this.plant = plant
                        updateUI(plant)
                    }
                } else {
                    Toast.makeText(context, "해당 식물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun updateUI(plant: Plant) {
        binding.apply {
            plantName.text = plant.name
            plantDescription.text = plant.description
            registrationDate.text = SimpleDateFormat("yyyy년 MM월 dd일 등록", Locale.getDefault())
                .format(Date(plant.registrationDate))

            Glide.with(requireContext())
                .load(plant.imagePath)
                .into(plantImage)

            // 일기 목록 및 댓글 표시
            diaryEntriesLayout.removeAllViews()
            plant.diaryEntries.forEach { entry ->
                val entryView = layoutInflater.inflate(R.layout.item_diary_entry, diaryEntriesLayout, false)

                // 일기 내용 설정
                entryView.findViewById<TextView>(R.id.entryContent).text = entry.content
                entryView.findViewById<TextView>(R.id.entryDate).text =
                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(entry.date))

                // 이미지가 있다면 표시
                if (entry.imagePath != null) {
                    val imageView = entryView.findViewById<ImageView>(R.id.diaryImage)
                    imageView.visibility = View.VISIBLE
                    Glide.with(requireContext())
                        .load(entry.imagePath)
                        .into(imageView)
                }

                // 댓글 입력 및 표시 설정
                val commentInput = entryView.findViewById<EditText>(R.id.commentInput)
                val sendButton = entryView.findViewById<ImageButton>(R.id.sendCommentButton)
                val commentsContainer = entryView.findViewById<LinearLayout>(R.id.commentsContainer)

                // 기존 댓글 표시
                updateComments(commentsContainer, entry.comments)

                // 댓글 전송 버튼 클릭 리스너
                sendButton.setOnClickListener {
                    val commentText = commentInput.text.toString()
                    if (commentText.isNotEmpty()) {
                        addComment(entry.id, commentText, commentsContainer)
                        commentInput.text.clear()
                    }
                }

                diaryEntriesLayout.addView(entryView)
            }
        }
    }
    private fun updateComments(container: LinearLayout, comments: List<DiaryComment>) {
        container.removeAllViews()
        comments.forEach { comment ->
            val commentView = layoutInflater.inflate(R.layout.item_comment, container, false)

            commentView.findViewById<TextView>(R.id.userName).text = comment.userName
            commentView.findViewById<TextView>(R.id.commentContent).text = comment.content
            commentView.findViewById<TextView>(R.id.commentTime).text =
                SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                    .format(Date(comment.timestamp))

            container.addView(commentView)
        }
    }

    private fun addComment(diaryEntryId: String, content: String, container: LinearLayout) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val comment = DiaryComment(
            userId = currentUser.uid,
            userName = currentUser.displayName ?: "Unknown",
            content = content
        )

        // Firestore에 댓글 추가
        firestore.collection("plants").document(plantId)
            .get()
            .addOnSuccessListener { document ->
                val plant = document.toObject(Plant::class.java)
                plant?.let {
                    // 해당 일기 찾기 및 댓글 추가
                    val diaryEntry = it.diaryEntries.find { entry -> entry.id == diaryEntryId }
                    diaryEntry?.comments?.add(comment)

                    // Firestore 업데이트
                    firestore.collection("plants").document(plantId)
                        .set(plant)
                        .addOnSuccessListener {
                            // 새 댓글 UI 추가
                            updateComments(container, diaryEntry?.comments ?: listOf())
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
    }



    companion object {
        private const val ARG_PLANT_ID = "plant_id"

        fun newInstance(plantId: String): PlantDetailFragment {
            return PlantDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLANT_ID, plantId) // plantId를 번들에 저장
                }
            }
        }
    }
}