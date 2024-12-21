package com.example.shickjip.ui

import android.os.Bundle
import android.util.Log
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
                Toast.makeText(context, "Please enter a valid email.", Toast.LENGTH_SHORT).show()
            }
        }

        // 취소 버튼 이벤트 처리
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
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

    private fun validateAndAddFriend(friendEmail: String) {
        currentUser?.let { user ->
            firestore.collection("users").whereEqualTo("email", friendEmail).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(context, "No user found with this email.", Toast.LENGTH_SHORT).show()
                    } else {
                        addFriendToFirestore(friendEmail)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error validating email: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addFriendToFirestore(friendEmail: String) {
        currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentFriends = snapshot.get("friends") as? MutableList<String> ?: mutableListOf()

                if (!currentFriends.contains(friendEmail)) {
                    currentFriends.add(friendEmail)
                    transaction.update(userRef, "friends", currentFriends)
                } else {
                    throw Exception("Friend already added.")
                }
            }.addOnSuccessListener {
                Toast.makeText(context, "Friend added successfully!", Toast.LENGTH_SHORT).show()

                // ArchiveFragment에서 친구 목록을 다시 로드하도록 설정
                parentFragmentManager.setFragmentResult(
                    "friendAdded",
                    Bundle().apply { putString("newFriendEmail", friendEmail) }
                )
                dismiss()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add friend: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}