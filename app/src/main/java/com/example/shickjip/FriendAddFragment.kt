package com.example.shickjip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.shickjip.databinding.FragmentFriendAddBinding

class FriendAddFragment : DialogFragment() {
    private var _binding: FragmentFriendAddBinding? = null
    private val binding get() = _binding!!

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

        // 친구 추가 버튼 클릭 이벤트
        binding.btnAddFriend.setOnClickListener {
            val friendName = binding.etFriendName.text.toString()
            if (friendName.isNotBlank()) {
                // Firebase에 친구 추가 로직
                Toast.makeText(context, "Friend '$friendName' added!", Toast.LENGTH_SHORT).show()
                dismiss() // 모달 닫기
            } else {
                Toast.makeText(context, "Please enter a name.", Toast.LENGTH_SHORT).show()
            }
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