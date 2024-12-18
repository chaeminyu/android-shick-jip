package com.example.shickjip

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.shickjip.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var currentSelectedIndex = 1 // 기본값: HomeFragment 인덱스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewPager 설정
        val adapter = HomeBNVAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = currentSelectedIndex

        // BottomNavigationView 화면 전환
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedIndex = when (item.itemId) {
                R.id.menu_archive -> 0
                R.id.menu_home -> 1
                R.id.menu_mypage -> 2
                else -> return@setOnItemSelectedListener false
            }

            if (selectedIndex != currentSelectedIndex) {
                binding.viewPager.setCurrentItem(selectedIndex, true)
                currentSelectedIndex = selectedIndex
            }
            true
        }

        // ViewPager 페이지 변경 리스너 설정
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSelectedIndex = position
                binding.bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })

        // 홈 버튼 클릭 리스너
        binding.homeButtonIcon.setOnClickListener {
            val homeIndex = 1
            if (currentSelectedIndex != homeIndex) {
                binding.viewPager.setCurrentItem(homeIndex, true)
                currentSelectedIndex = homeIndex
            }
        }
    }
}