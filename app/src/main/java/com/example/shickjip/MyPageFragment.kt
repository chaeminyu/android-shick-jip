package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.shickjip.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyPageFragment : Fragment() {
    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentUser: UserProfile? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfile()
        setupClickListeners()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun loadUserProfile() {
        auth.currentUser?.let { user ->
            // users 컬렉션에서 현재 로그인한 사용자의 uid로 직접 문서를 조회
            firestore.collection("users")
                .document(user.uid) // email 대신 uid 사용
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("MyPageFragment", "Error fetching user data", e)
                        Toast.makeText(context, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val userProfile = snapshot.toObject(UserProfile::class.java)
                        currentUser = userProfile
                        updateUI(userProfile)
                    }
                }
        }
    }
    // UI 업데이트 함수 수정
    private fun updateUI(userProfile: UserProfile?) {
        userProfile?.let { profile ->
            binding.apply {
                welcomeText.text = "${profile.username}님, 환영합니다!"
                coinAmount.text = profile.coins.toString()

                // 레벨 프로그레스바 업데이트
                val nextLevelExp = calculateNextLevelExp(profile.level)
                val progress = (profile.experience.toFloat() / nextLevelExp * 100).toInt()
                levelProgressBar.progress = progress
            }
        }
    }
    private fun calculateNextLevelExp(currentLevel: Int): Int {
        // 레벨별 필요 경험치 계산 로직
        return when (currentLevel) {
            1 -> 500
            2 -> 1000
            3 -> 1500
            4 -> 2000
            else -> 2500
        }
    }
    private fun processCoinCharge(amount: Int) {
        val currentUser = auth.currentUser ?: return

        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("users").document(currentUser.uid)
            val snapshot = transaction.get(userRef)
            val currentCoins = snapshot.getLong("coins") ?: 0

            transaction.update(userRef, "coins", currentCoins + amount)
            // 경험치도 함께 증가
            val currentExp = snapshot.getLong("experience") ?: 0
            transaction.update(userRef, "experience", currentExp + (amount / 10))
        }.addOnSuccessListener {
            Toast.makeText(context, "${amount}코인이 충전되었습니다", Toast.LENGTH_SHORT).show()
            loadUserProfile() // UI 새로고침
        }.addOnFailureListener { e ->
            Toast.makeText(context, "충전 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfile(newPassword: String, newNickname: String) {
        val currentUser = auth.currentUser ?: return

        // 비밀번호 업데이트
        currentUser.updatePassword(newPassword)
            .addOnCompleteListener { passwordTask ->
                if (passwordTask.isSuccessful) {
                    // Firestore 사용자 정보 업데이트
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update("username", newNickname)
                        .addOnSuccessListener {
                            Toast.makeText(context, "회원 정보가 수정되었습니다", Toast.LENGTH_SHORT).show()
                            binding.welcomeText.text = "${newNickname}님, 환영합니다!"
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "닉네임 변경 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "비밀번호 변경 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun processWithdrawal() {
        val currentUser = auth.currentUser ?: return

        // Firestore 데이터 삭제
        firestore.collection("users")
            .document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                // Firebase Auth 계정 삭제
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "회원 탈퇴가 완료되었습니다", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "계정 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "데이터 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    // 코인 충전 다이얼로그 함수 수정
    private fun showCoinChargeDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val bindingDialog = DialogCoinChargeBinding.inflate(layoutInflater)
        dialog.setContentView(bindingDialog.root)

        var selectedAmount = 0

        bindingDialog.apply {
            // 라디오 버튼 클릭 리스너
            chargeAmountGroup.setOnCheckedChangeListener { _, checkedId ->
                selectedAmount = when (checkedId) {
                    R.id.amount1000 -> 1000
                    R.id.amount5000 -> 5000
                    R.id.amount10000 -> 10000
                    else -> 0
                }
                // 금액 선택 시 충전 버튼 활성화
                chargeButton.isEnabled = selectedAmount > 0
            }

            // 충전 버튼
            chargeButton.isEnabled = false // 초기 상태는 비활성화
            chargeButton.setOnClickListener {
                if (selectedAmount > 0) {
                    chargeCoin(selectedAmount) { success ->
                        if (success) {
                            dialog.dismiss()
                        }
                    }
                }
            }

            // 취소 버튼
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
    // 코인 충전 처리 함수 추가
    private fun chargeCoin(amount: Int, onComplete: (Boolean) -> Unit) {
        auth.currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)

            // 먼저 문서 존재 여부 확인
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // 문서가 존재하면 트랜잭션으로 업데이트
                    firestore.runTransaction { transaction ->
                        val snapshot = transaction.get(userRef)
                        val currentCoins = snapshot.getLong("coins") ?: 0
                        val newCoins = currentCoins + amount

                        transaction.update(userRef, "coins", newCoins)
                        true
                    }.addOnSuccessListener { success ->
                        if (success) {
                            Toast.makeText(context, "${amount}코인이 충전되었습니다", Toast.LENGTH_SHORT).show()
                            binding.coinAmount.text = (binding.coinAmount.text.toString().toIntOrNull() ?: 0 + amount).toString()
                            onComplete(true)
                        }
                    }.addOnFailureListener { e ->
                        Log.e("MyPageFragment", "Error charging coins", e)
                        Toast.makeText(context, "충전 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        onComplete(false)
                    }
                } else {
                    // 문서가 없으면 새로 생성
                    val userProfile = hashMapOf(
                        "id" to user.uid,
                        "username" to (user.displayName ?: "User"),
                        "email" to (user.email ?: ""),
                        "level" to 1,
                        "experience" to 0,
                        "coins" to amount.toLong(),
                        "registrationDate" to System.currentTimeMillis()
                    )

                    userRef.set(userProfile)
                        .addOnSuccessListener {
                            Toast.makeText(context, "${amount}코인이 충전되었습니다", Toast.LENGTH_SHORT).show()
                            binding.coinAmount.text = amount.toString()
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("MyPageFragment", "Error creating user profile", e)
                            Toast.makeText(context, "프로필 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("MyPageFragment", "Error checking document", e)
                Toast.makeText(context, "문서 확인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        } ?: run {
            Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            onComplete(false)
        }
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
}