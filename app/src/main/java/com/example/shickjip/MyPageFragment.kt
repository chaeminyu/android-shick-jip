package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.shickjip.databinding.FragmentMypageBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.shickjip.databinding.DialogCoinChargeBinding
import com.example.shickjip.databinding.DialogEditProfileBinding
import com.example.shickjip.databinding.DialogWithdrawalBinding

class MyPageFragment : Fragment() {
    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 레벨에 따른 프로그레스바 업데이트
        updateLevelProgress()
        setupClickListeners()
    }

    private fun updateLevelProgress() {
        // 예시: 현재 경험치가 1980이고 다음 레벨까지 2000이 필요한 경우
        val currentExp = 1980
        val maxExp = 2000
        val progress = (currentExp.toFloat() / maxExp * 100).toInt()

        binding.levelProgressBar.max = 100
        binding.levelProgressBar.progress = progress
    }

    private fun setupClickListeners() {
        // 식물 인식하기 버튼 (CardView)
        binding.scanButtonLayout.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }

        // 도감 읽기 버튼 (CardView)
        binding.archiveButton.setOnClickListener {
            (activity as? HomeActivity)?.binding?.viewPager?.currentItem = 0  // ArchiveFragment의 인덱스
        }

        // 코인 충전하기 카드
        binding.coinCard.setOnClickListener {
            showCoinChargeDialog()
        }

        // 홈으로 버튼
        binding.homeButton.setOnClickListener {
            (activity as? HomeActivity)?.binding?.viewPager?.currentItem = 1  // HomeFragment의 인덱스
        }

        // 샵으로 이동하는 버튼 클릭 리스너
        binding.shopButton.setOnClickListener {
            (activity as? HomeActivity)?.showShopFragment()
        }


        // 회원 수정 버튼
        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        // 회원 탈퇴 버튼
        binding.deleteAccountButton.setOnClickListener {
            showWithdrawalDialog()
        }
    }

    private fun showCoinChargeDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val bindingDialog = DialogCoinChargeBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        // 선택된 금액 저장 변수
        var selectedAmount = 0

        // 라디오 버튼 클릭 리스너 설정
        bindingDialog.apply {
            amount1000.setOnClickListener { selectedAmount = 1000 }
            amount5000.setOnClickListener { selectedAmount = 5000 }
            amount10000.setOnClickListener { selectedAmount = 10000 }

            // 취소 버튼
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            // 충전 버튼
            chargeButton.setOnClickListener {
                if (selectedAmount > 0) {
                    // TODO: 실제 결제 처리 로직 구현
                    processCoinCharge(selectedAmount)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "충전할 금액을 선택해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun processCoinCharge(amount: Int) {
        // TODO: 실제 결제 처리 및 DB 업데이트
        // 임시로 토스트 메시지만 표시
        Toast.makeText(
            requireContext(),
            "${amount}코인이 충전되었습니다",
            Toast.LENGTH_SHORT
        ).show()

        // 코인 잔액 UI 업데이트
        updateCoinBalance(amount)
    }

    private fun updateCoinBalance(addedAmount: Int) {
        val currentBalance = binding.coinAmount.text.toString().toIntOrNull() ?: 0
        val newBalance = currentBalance + addedAmount
        binding.coinAmount.text = newBalance.toString()
    }

    private fun showEditProfileDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val bindingDialog = DialogEditProfileBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        bindingDialog.apply {
            confirmButton.setOnClickListener {
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val newNickname = newNicknameInput.text.toString()

                if (currentPassword.isBlank() || newPassword.isBlank() || newNickname.isBlank()) {
                    Toast.makeText(requireContext(), "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // TODO: 비밀번호 유효성 검사 및 실제 정보 업데이트 로직 구현
                updateUserProfile(newPassword, newNickname)
                dialog.dismiss()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateUserProfile(newPassword: String, newNickname: String) {
        // TODO: 실제 DB 업데이트 로직 구현
        Toast.makeText(requireContext(), "회원 정보가 수정되었습니다", Toast.LENGTH_SHORT).show()

        // 닉네임 UI 업데이트
        binding.welcomeText.text = "${newNickname}님, 환영합니다!"
    }

    private fun showWithdrawalDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val bindingDialog = DialogWithdrawalBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        bindingDialog.apply {
            confirmButton.setOnClickListener {
                // 한번 더 확인하는 다이얼로그 표시
                AlertDialog.Builder(requireContext())
                    .setTitle("회원 탈퇴")
                    .setMessage("정말로 탈퇴하시겠습니까?\n모든 데이터가 삭제됩니다.")
                    .setPositiveButton("확인") { _, _ ->
                        processWithdrawal()
                        dialog.dismiss()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun processWithdrawal() {
        // TODO: 실제 회원 탈퇴 처리 로직 구현
        Toast.makeText(requireContext(), "회원 탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()

        // MainActivity로 이동
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }
}