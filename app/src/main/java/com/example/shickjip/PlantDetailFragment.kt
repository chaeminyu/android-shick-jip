package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
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
        setupListeners()
    }
    private fun setupListeners() {
        // 닉네임 변경 버튼 클릭
        binding.editNicknameButton.setOnClickListener {
            binding.nicknameEditText.visibility = View.VISIBLE // EditText 표시
            binding.saveNicknameButton.visibility = View.VISIBLE // 저장 버튼 표시
            binding.editNicknameButton.visibility = View.GONE // 변경 버튼 숨김
        }

        // 닉네임 저장 버튼 클릭
        binding.saveNicknameButton.setOnClickListener {
            val newNickname = binding.nicknameEditText.text.toString().trim()
            if (newNickname.isNotEmpty()) {
                plantId?.let { id ->
                    updateNickname(id, newNickname)
                }
            } else {
                Toast.makeText(requireContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViews() {
        binding.backButton.setOnClickListener {
            // 뒤로가기 시 ViewPager 표시
            (requireActivity() as HomeActivity).binding.viewPager.visibility = View.VISIBLE
            parentFragmentManager.popBackStack()
        }

        binding.writeDiaryButton.setOnClickListener {
            // 일기 작성 페이지로 이동
            parentFragmentManager.beginTransaction()
                .replace(R.id.shopFragmentContainer, DiaryWritingFragment.newInstance(plantId))
                .addToBackStack(null)
                .commit()
        }
    }

    private var snapshotListener: ListenerRegistration? = null

    private fun loadPlantDetails() {
        firestore.collection("plants").document(plantId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PlantDetailFragment", "Error fetching plant details", e)
                    if (isAdded) {
                        Toast.makeText(context, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(Plant::class.java)?.let { plant ->
                        this.plant = plant
                        if (isAdded && binding != null) { // Fragment가 아직 활성 상태인지 확인
                            updateUI(plant)
                        } else {
                            Log.w("PlantDetailFragment", "Fragment is not attached or binding is null")
                        }
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(context, "해당 식물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }


    private fun updateUI(plant: Plant) {
        if (!isAdded || binding == null) {
            Log.w("PlantDetailFragment", "Fragment is not added or binding is null, skipping UI update")
            return
        }

        binding.apply {
            val displayName = if (plant.nickname.isNotEmpty()) {
                plant.nickname
            } else if (plant.name.contains(" (")) {
                plant.name.split(" (")[0]
            } else {
                plant.name
            }

            plantName.text = displayName
            plantDescription.text = plant.description
            registrationDate.text = SimpleDateFormat("yyyy년 MM월 dd일 등록", Locale.getDefault())
                .format(Date(plant.registrationDate))

            if (plant.imagePath.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(plant.imagePath)
                    .error(R.drawable.error_image)
                    .into(plantImage)
            }

            diaryEntriesLayout.removeAllViews()
            plant.diaryEntries.forEach { entry ->
                val entryView = layoutInflater.inflate(R.layout.item_diary_entry, diaryEntriesLayout, false)
                entryView.findViewById<TextView>(R.id.entryContent).text = entry.content
                entryView.findViewById<TextView>(R.id.entryDate).text =
                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(entry.date))

                if (entry.imagePath != null) {
                    val imageView = entryView.findViewById<ImageView>(R.id.diaryImage)
                    imageView.visibility = View.VISIBLE
                    Glide.with(requireContext())
                        .load(entry.imagePath)
                        .into(imageView)
                }

                val commentInput = entryView.findViewById<EditText>(R.id.commentInput)
                val sendButton = entryView.findViewById<ImageButton>(R.id.sendCommentButton)
                val commentsContainer = entryView.findViewById<LinearLayout>(R.id.commentsContainer)

                updateComments(commentsContainer, entry.comments)

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

    private fun updateNickname(plantId: String, newNickname: String) {
        // Firebase 참조 가져오기
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("plants").document(plantId)
            .update("nickname", newNickname)
            .addOnSuccessListener {
                binding.nicknameEditText.visibility = View.GONE
                binding.saveNicknameButton.visibility = View.GONE
                binding.editNicknameButton.visibility = View.VISIBLE
                binding.plantName.text = newNickname
                Toast.makeText(requireContext(), "닉네임이 저장되었습니다!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                // 권한 문제인지 확인
                if (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Toast.makeText(requireContext(), "친구의 닉네임을 변경할 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "닉네임 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
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

        // Firestore에서 현재 사용자의 username을 가져온 후 댓글 작성
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username") ?: "Unknown"

                val comment = DiaryComment(
                    userId = currentUser.uid,
                    userName = username,
                    content = content
                )

                // 기존의 댓글 추가 로직
                firestore.collection("plants").document(plantId)
                    .get()
                    .addOnSuccessListener { plantDoc ->
                        val plant = plantDoc.toObject(Plant::class.java)
                        plant?.let {
                            val diaryEntry = it.diaryEntries.find { entry -> entry.id == diaryEntryId }
                            diaryEntry?.comments?.add(comment)

                            // 댓글 Firestore에 저장
                            firestore.collection("plants").document(plantId)
                                .set(plant)
                                .addOnSuccessListener {
                                    // 댓글 UI 업데이트
                                    updateComments(container, diaryEntry?.comments ?: listOf())

                                    // 경험치 5 추가
                                    val userRef = firestore.collection("users").document(currentUser.uid)
                                    firestore.runTransaction { transaction ->
                                        val snapshot = transaction.get(userRef)
                                        val currentExperience = snapshot.getLong("experience") ?: 0
                                        transaction.update(userRef, "experience", currentExperience + 5)
                                    }.addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "댓글 작성 완료! 경험치 +5 획득!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.addOnFailureListener { e ->
                                        Log.e("PlantDetailFragment", "경험치 업데이트 실패", e)
                                        Toast.makeText(
                                            context,
                                            "경험치 업데이트에 실패했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PlantDetailFragment", "플랜트 데이터를 가져오는데 실패", e)
                        Toast.makeText(context, "댓글 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("PlantDetailFragment", "사용자 정보를 가져오는데 실패", e)
                Toast.makeText(context, "사용자 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        snapshotListener = null
        requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
        requireActivity().findViewById<FrameLayout>(R.id.shopFragmentContainer).visibility = View.GONE
        _binding = null
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