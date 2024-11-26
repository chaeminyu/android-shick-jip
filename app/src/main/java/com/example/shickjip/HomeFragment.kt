package com.example.shickjip

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.shickjip.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private lateinit var bannerAdapter: BannerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 배너 데이터 설정
        val banners = listOf(
            Banner(R.drawable.banner_flower, "Editor's pick! 오늘의 식물", "성년의 날을 기념하며 전해지는 장미는 단순한 꽃을 넘어 새로운 시작을 응원하는 특별한 의미를 지니고 있습니다. 오늘, 성년의 날을 맞아 스스로의 성장을 축하하며 장미의 향기와 함께 새로운 여정을 시작해보세요!"),
            Banner(R.drawable.banner_flower, "Editor's pick! 오늘의 식물", "성년의 날을 기념하며 전해지는 장미는 단순한 꽃을 넘어 새로운 시작을 응원하는 특별한 의미를 지니고 있습니다. 오늘, 성년의 날을 맞아 스스로의 성장을 축하하며 장미의 향기와 함께 새로운 여정을 시작해보세요!"),
            Banner(R.drawable.banner_flower, "Editor's pick! 오늘의 식물", "성년의 날을 기념하며 전해지는 장미는 단순한 꽃을 넘어 새로운 시작을 응원하는 특별한 의미를 지니고 있습니다. 오늘, 성년의 날을 맞아 스스로의 성장을 축하하며 장미의 향기와 함께 새로운 여정을 시작해보세요!")
        )

        // 배너 어댑터 설정
        bannerAdapter = BannerAdapter(banners)
        binding.viewpager.adapter = bannerAdapter

        // 자동 슬라이드 시작
        handler = Handler(Looper.getMainLooper())
        startAutoSlide(banners.size)
    }

    private fun startAutoSlide(itemCount: Int) {
        val runnable = object : Runnable {
            override fun run() {
                val nextItem = (binding.viewpager.currentItem + 1) % itemCount
                binding.viewpager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000) // 3초 간격으로 슬라이드
            }
        }
        handler.postDelayed(runnable, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // 자동 슬라이드 중지
        _binding = null
    }
}