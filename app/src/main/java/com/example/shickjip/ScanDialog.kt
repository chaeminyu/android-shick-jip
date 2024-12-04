package com.example.shickjip

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.shickjip.databinding.ItemScanmodalBinding

class ScanDialog(
    private var state: ModalState,
    private val onDismiss: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: ItemScanmodalBinding? = null
    private val binding get() = _binding!!
    private var rotationAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ItemScanmodalBinding.inflate(inflater, container, false)
        val view = binding.root

        // 다이얼로그 배경을 투명하게 설정
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 상태에 따라 UI 업데이트
        updateModalUI(state)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 3초 후 다이얼로그 자동 종료
        Handler(Looper.getMainLooper()).postDelayed({
            dismiss() // 다이얼로그 닫기
            onDismiss?.invoke() // 콜백 실행
        }, 2000)
    }

    private fun updateModalUI(state: ModalState) {
        when (state) {
            ModalState.LOADING -> {
                binding.modalBg.setImageResource(R.drawable.modal_bg_loading)
                binding.modalIcon.setImageResource(R.drawable.ic_modal_loading)
                binding.modalTitle.text = "로딩 중입니다..."
                binding.modalMessage.text = "잠시만 기다려주세요!"
                rotationAnimator = ObjectAnimator.ofFloat(binding.modalIcon, "rotation", 0f, 360f).apply {
                    duration = 2000 // 1초 동안 한 바퀴 회전
                    repeatCount = ObjectAnimator.INFINITE // 무한 반복
                    repeatMode = ObjectAnimator.RESTART
                    start()
                }
            }
            ModalState.SUCCESS -> {
                binding.modalBg.setImageResource(R.drawable.modal_bg_success)
                binding.modalIcon.setImageResource(R.drawable.ic_modal_success)
                binding.modalTitle.text = "식물을 찾았어요!"
                binding.modalMessage.text = "이제 이 친구에 대해 알려드릴게요!"
            }
            ModalState.FAILURE -> {
                binding.modalBg.setImageResource(R.drawable.modal_bg_failure)
                binding.modalIcon.setImageResource(R.drawable.ic_modal_failure)
                binding.modalTitle.text = "식물을 찾지 못했어요..."
                binding.modalMessage.text = "다시 한 번 촬영해볼까요?"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class ModalState {
    LOADING, SUCCESS, FAILURE
}