package com.example.shickjip

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shickjip.databinding.FragmentShopBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    // SharedPreferences 키
    private val PREFS_NAME = "user_prefs"
    private val KEY_PURCHASED_THEMES = "purchased_themes"

    // 특별 테마 설정
    private val specialTheme = ThemeItem(
        id = "special_theme_1",
        title = "기간 한정 세일 테마!",
        description = "11월 30일까지 반값 테마!",
        imageResId = R.drawable.shop_special,
        price = 300
    )

    // 기본 테마 리스트 설정
    private val basicThemes = listOf(
        ThemeItem(
            id = "basic_theme_1",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500
        ),
        ThemeItem(
            id = "basic_theme_2",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500
        ),
        ThemeItem(
            id = "basic_theme_3",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500
        ),
        ThemeItem(
            id = "basic_theme_4",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500
        ),
        ThemeItem(
            id = "basic_theme_5",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500
        )
    )

    private lateinit var adapter: ThemeAdapter
    private var userCoins: Int = 0 // 코인 저장 변수 (전역 변수로 선언)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 코인 불러오기
        loadUserCoins()

        // 결제된 상품 불러오기
        val purchasedThemes = getPurchasedThemes()
        markPurchasedThemes(purchasedThemes)

        // RecyclerView 설정
        adapter = ThemeAdapter(
            themes = basicThemes,
            onPaymentClick = { themeItem ->
                showPaymentDialog(themeItem)
            },
            onApplyClick = { themeItem ->
                applyTheme(themeItem)
            }
        )

        binding.recyclerViewThemes.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewThemes.adapter = adapter


        // 한정 특가 테마 뷰 바인딩
        binding.specialThemeTitle.text = specialTheme.title
        binding.specialThemeDescription.text = specialTheme.description
        binding.specialThemeImage.setImageResource(specialTheme.imageResId)
        binding.price.text = "${specialTheme.price}"


        // 스페셜 테마 버튼 설정
        updateSpecialThemeButton()

        binding.specialThemeButton.setOnClickListener {
            if (specialTheme.isPurchased) {
                applyTheme(specialTheme)
            } else {
                showPaymentDialog(specialTheme)
            }
        }

        // 뒤로 가기 버튼 클릭 리스너
        binding.backIcon.setOnClickListener {
            (activity as? HomeActivity)?.hideShopFragment()
        }

        // 하드웨어 뒤로 가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as? HomeActivity)?.hideShopFragment()
            }
        })
    }

    private fun showPaymentDialog(themeItem: ThemeItem) {
        val price = themeItem.price

        val paymentDialog = PaymentDialog(userCoins, price) {
            processPayment(price, themeItem)
        }

        paymentDialog.show(parentFragmentManager, "PaymentDialog")
    }

    private fun processPayment(price: Int, themeItem: ThemeItem) {
        if (userCoins >= price) {
            val newBalance = userCoins - price
            updateUserCoins(newBalance) // Firebase로 업데이트
            savePurchasedTheme(themeItem.id) // 구매 정보는 SharedPreferences에 저장
            themeItem.isPurchased = true
            Toast.makeText(requireContext(), "결제가 완료되었습니다!", Toast.LENGTH_SHORT).show()

            adapter.notifyDataSetChanged()
            updateSpecialThemeButton()
        } else {
            Toast.makeText(requireContext(), "코인이 부족합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserCoins() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = firestore.collection("users").document(currentUser.uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    userCoins = document.getLong("coins")?.toInt() ?: 0 // 기본 값 설정
                    binding.userCoin.text = userCoins.toString()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "코인 데이터를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserCoins(newCoins: Int) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = firestore.collection("users").document(currentUser.uid)
            userRef.update("coins", newCoins)
                .addOnSuccessListener {
                    userCoins = newCoins
                    binding.userCoin.text = userCoins.toString()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "코인 업데이트 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun applyTheme(themeItem: ThemeItem) {
        Toast.makeText(requireContext(), "${themeItem.title}가 적용되었습니다!", Toast.LENGTH_SHORT).show()
    }


    private fun markPurchasedThemes(purchasedThemes: Set<String>) {
        if (purchasedThemes.contains(specialTheme.id)) {
            specialTheme.isPurchased = true
        }
        basicThemes.forEach { theme ->
            theme.isPurchased = purchasedThemes.contains(theme.id)
        }
    }

    private fun savePurchasedTheme(themeId: String) {
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val purchasedThemes = getPurchasedThemes().toMutableSet()
        purchasedThemes.add(themeId) // ID로 저장
        sharedPrefs.edit().putStringSet(KEY_PURCHASED_THEMES, purchasedThemes).apply()
    }

    private fun getPurchasedThemes(): Set<String> {
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getStringSet(KEY_PURCHASED_THEMES, emptySet()) ?: emptySet()
    }

    private fun updateSpecialThemeButton() {
        if (specialTheme.isPurchased) {
            binding.specialThemeButton.text = "적용하기"
            binding.specialThemeButton.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.green5) // 버튼 배경 색상
            )
            binding.specialThemeButton.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.white)  // 버튼 텍스트 색상
            )

            binding.specialThemeButton.strokeColor = ContextCompat.getColorStateList(binding.root.context, R.color.green5) // 테두리 색상 변경

            binding.specialThemeButton.setOnClickListener {
                applyTheme(specialTheme)
            }
        } else {
            binding.specialThemeButton.text = "결제하기"
            binding.specialThemeButton.setOnClickListener {
                showPaymentDialog(specialTheme)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}