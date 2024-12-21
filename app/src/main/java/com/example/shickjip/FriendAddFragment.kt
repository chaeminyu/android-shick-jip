package com.example.shickjip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
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

        // 닫기 버튼
        binding.btnCloseModal.setOnClickListener {
            dismiss()
        }

        // 친구 추가 버튼
        binding.btnAddFriend.setOnClickListener {
            val friendEmail = binding.etFriendName.text.toString().trim()
            if (friendEmail.isNotBlank()) {
                addFriendToFirestore(friendEmail)
            } else {
                Toast.makeText(context, "Please enter an email address.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addFriendToFirestore(friendEmail: String) {
        val userEmail = currentUser?.email

        if (currentUser == null || userEmail == null) {
            Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the current user's document
        val userDocRef = firestore.collection("users").document(currentUser!!.uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val friendsList = snapshot.get("friends") as? MutableList<String> ?: mutableListOf()

            // 이미 친구로 등록된 경우 예외 발생
            if (friendsList.contains(friendEmail)) {
                throw Exception("Friend already added.")
            }

            // 새 친구 추가
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
    }
}