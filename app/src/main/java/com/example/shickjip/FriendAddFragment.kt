package com.example.shickjip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.shickjip.R
import com.example.shickjip.databinding.FragmentFriendAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendAddFragment : DialogFragment() {
    private var _binding: FragmentFriendAddBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val currentUser by lazy { FirebaseAuth.getInstance().currentUser }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 닫기 버튼 이벤트 처리
        binding.btnCloseModal.setOnClickListener {
            dismiss()
        }

        // 친구 추가 버튼 이벤트 처리
        binding.btnAddFriend.setOnClickListener {
            val friendEmail = binding.etFriendName.text.toString().trim()
            if (friendEmail.isNotBlank()) {
                validateAndAddFriend(friendEmail)
            } else {
                Toast.makeText(context, "Please enter an email address.", Toast.LENGTH_SHORT).show()
            }
        }

        // 취소 버튼 이벤트 처리
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun validateAndAddFriend(friendEmail: String) {
        val userEmail = currentUser?.email

        if (currentUser == null || userEmail == null) {
            Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 1: Firestore에서 friendEmail이 유효한 사용자인지 확인
        firestore.collection("users")
            .whereEqualTo("email", friendEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(context, "The email '$friendEmail' does not belong to a valid user.", Toast.LENGTH_SHORT).show()
                } else {
                    addFriendToFirestore(friendEmail)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error checking user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addFriendToFirestore(friendEmail: String) {
        val userDocRef = firestore.collection("users").document(currentUser!!.uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val friendsList = snapshot.get("friends") as? MutableList<String> ?: mutableListOf()

            if (friendsList.contains(friendEmail)) {
                throw Exception("Friend already added.")
            }

            friendsList.add(friendEmail)
            transaction.update(userDocRef, "friends", friendsList)
        }.addOnSuccessListener {
            Toast.makeText(context, "Friend '$friendEmail' added successfully!", Toast.LENGTH_SHORT).show()
            dismiss() // 모달 닫기
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to add friend: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.apply {
            // 디스플레이 너비의 85%로 설정
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()
            setLayout(
                width,  // 너비를 화면 너비의 85%로 설정
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // 배경 설정
            setBackgroundDrawableResource(R.drawable.modal_background)
        }
    }
}