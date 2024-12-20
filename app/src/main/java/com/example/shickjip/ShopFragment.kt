package com.example.shickjip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shickjip.databinding.FragmentShopBinding

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeList = listOf(
            ThemeItem(
                title = "크리스마스 한정 산타 테마!",
                description = "12/25까지 산타 테마를 즐겨보아요",
                imageResId = R.drawable.theme_winter,
                price = 500
            ),
            ThemeItem(
                title = "크리스마스 한정 산타 테마!",
                description = "12/25까지 산타 테마를 즐겨보아요",
                imageResId = R.drawable.theme_winter,
                price = 500
            ),
            ThemeItem(
                title = "크리스마스 한정 산타 테마!",
                description = "12/25까지 산타 테마를 즐겨보아요",
                imageResId = R.drawable.theme_winter,
                price = 500
            ),
            ThemeItem(
                title = "크리스마스 한정 산타 테마!",
                description = "12/25까지 산타 테마를 즐겨보아요",
                imageResId = R.drawable.theme_winter,
                price = 500
            ),
            ThemeItem(
                title = "크리스마스 한정 산타 테마!",
                description = "12/25까지 산타 테마를 즐겨보아요",
                imageResId = R.drawable.theme_winter,
                price = 500
            )
            // 필요에 따라 추가 아이템 추가
        )

        // RecyclerView 설정
        val adapter = ThemeAdapter(themeList)
        binding.recyclerViewThemes.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewThemes.adapter = adapter

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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}