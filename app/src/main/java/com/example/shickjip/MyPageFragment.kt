package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        setupClickListeners()
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

        // 샵으로 버튼
        binding.shopButton.setOnClickListener {
            // ShopFragment로 이동하는 코드
            val shopFragment = ShopFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, shopFragment)
                .addToBackStack(null)
                .commit()
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

        bindingDialog.apply {
            // 코인 충전 금액 선택 로직
            chargeButton.setOnClickListener {
                // 코인 충전 처리
                dialog.dismiss()
            }
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditProfileDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val bindingDialog = DialogEditProfileBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        bindingDialog.apply {
            confirmButton.setOnClickListener {
                // 회원 정보 수정 처리
                dialog.dismiss()
            }
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showWithdrawalDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val bindingDialog = DialogWithdrawalBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        bindingDialog.apply {
            confirmButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("회원 탈퇴")
                    .setMessage("정말로 탈퇴하시겠습니까?")
                    .setPositiveButton("확인") { _, _ ->
                        // 탈퇴 처리 및 로그인 화면으로 이동
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    }
                    .setNegativeButton("취소", null)
                    .show()
                dialog.dismiss()
            }
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}