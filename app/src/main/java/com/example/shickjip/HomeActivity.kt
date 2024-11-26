package com.example.shickjip

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.shickjip.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 화면 설정: HomeFragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // 바텀 네비게이션 화면 전환
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_archive -> {
                    replaceFragment(ArchiveFragment())
                    true
                }
                R.id.menu_home -> {
                    // 별도 동작 X
                    true
                }
                R.id.menu_mypage -> {
                    replaceFragment(MyPageFragment())
                    true
                }
                else -> false
            }
        }

        // 홈 버튼 클릭 리스너
        binding.homeButtonIcon.setOnClickListener {
            replaceFragment(HomeFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}