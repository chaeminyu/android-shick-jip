package com.example.shickjip

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.example.shickjip.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding
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
                // ViewPager2를 다시 보이도록 설정
                binding.viewPager.visibility = View.VISIBLE

                binding.viewPager.setCurrentItem(selectedIndex, true)
                currentSelectedIndex = selectedIndex
            }

            // FragmentContainerView 안의 프래그먼트를 제거 (PlantDetailFragment 제거)
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            true
        }

        // ViewPager 페이지 변경 리스너 설정
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSelectedIndex = position
                binding.bottomNavigationView.menu.getItem(position).isChecked = true

                // ViewPager2가 전환되면 FragmentContainerView를 초기화
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
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

    fun showShopFragment() {
        val shopFragment = ShopFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, 0) // 진입 애니메이션 적용
            .add(R.id.shopFragmentContainer, shopFragment)
            .addToBackStack("ShopFragment") // 백스택에 태그 추가
            .commit()

        // BottomNavigationView 숨기기
        binding.bottomNavigationView.visibility = View.GONE
        binding.shopFragmentContainer.visibility = View.VISIBLE
    }

    fun hideShopFragment() {
        val shopFragment = supportFragmentManager.findFragmentById(R.id.shopFragmentContainer)

        if (shopFragment != null) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_down) // 종료 애니메이션 적용
                .remove(shopFragment)
                .commit()

            // 애니메이션 지속 시간만큼 지연 후 Visibility 변경
            binding.shopFragmentContainer.postDelayed({
                binding.shopFragmentContainer.visibility = View.GONE
                binding.bottomNavigationView.visibility = View.VISIBLE
            }, 300) // 애니메이션 지속 시간과 동일하게 설정
        }
    }
}