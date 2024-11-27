package com.example.shickjip

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.shickjip.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var currentSelectedIndex = 1 // 기본값: HomeFragment 인덱스

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 화면 설정: HomeFragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), null)
        }

        // 바텀 네비게이션 화면 전환
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            // 각 메뉴에 해당하는 프래그먼트에 인덱스 부여
            val fragments = listOf(ArchiveFragment(), HomeFragment(), MyPageFragment())
            val selectedIndex = when (item.itemId) {
                R.id.menu_archive -> 0
                R.id.menu_home -> 1
                R.id.menu_mypage -> 2
                else -> return@setOnItemSelectedListener false
            }

            // 같은 탭을 누르면 동작 X
            if (selectedIndex != currentSelectedIndex) {
                // 애니메이션 설정
                val (inAnim, outAnim) = if (selectedIndex > currentSelectedIndex) {
                    R.anim.slide_in_right to R.anim.slide_out_left
                } else {
                    R.anim.slide_in_left to R.anim.slide_out_right
                }

                // 애니메이션 적용된 프래그먼트 전환
                replaceFragment(fragments[selectedIndex], inAnim to outAnim)
                currentSelectedIndex = selectedIndex // 인덱스 업데이트
            }
            true
        }

        // 홈 버튼 클릭 리스너
        binding.homeButtonIcon.setOnClickListener {
            val homeIndex = 1
            if (currentSelectedIndex != homeIndex) {
                val (inAnim, outAnim) = when {
                    currentSelectedIndex == 0 -> { // 도감 -> 홈
                        R.anim.slide_in_right to R.anim.slide_out_left
                    }

                    currentSelectedIndex == 2 -> { // 마이페이지 -> 홈
                        R.anim.slide_in_left to R.anim.slide_out_right
                    }

                    else -> { // else 처리
                        R.anim.slide_in_left to R.anim.slide_out_right
                    }
                }
                replaceFragment(HomeFragment(), inAnim to outAnim)
                currentSelectedIndex = homeIndex
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, animations: Pair<Int, Int>?) {
        val transaction = supportFragmentManager.beginTransaction()
        animations?.let { (inAnim, outAnim) ->
            transaction.setCustomAnimations(inAnim, outAnim)
        }
        transaction.replace(binding.fragmentContainer.id, fragment).commit()
    }
}