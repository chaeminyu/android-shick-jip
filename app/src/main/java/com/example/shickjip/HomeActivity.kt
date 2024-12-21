package com.example.shickjip

import ThemeItem
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.example.shickjip.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding
    private var currentSelectedIndex = 1 // 기본값: HomeFragment 인덱스
    private var isThemeRestored = false // 테마 복원이 완료되었는지 확인



    // 기본 테마 선언
    val defaultTheme = ThemeItem(
        id = "default",
        title = "기본 테마",
        description = "앱 기본 테마",
        imageResId = R.drawable.theme_winter, // 기본 이미지 설정
        price = 0,
        backgroundColor = Color.parseColor("#F3FAF3"), // 기본 배경 색상
        treeImageResId = R.drawable.home_tree, // 기본 트리 이미지
        hillImageResId = R.drawable.home_hill, // 기본 언덕 이미지
        buttonIconColor = Color.parseColor("#28532A"), // 기본 버튼 색상
        progressBarIndicatorColor = Color.parseColor("#358437"), // 기본 진행 색상
        progressBarTrackColor = Color.parseColor("#9DD89E") ,// 기본 트랙 색상
        textColor = Color.parseColor("#2D682F"), // TextView 텍스트 색상
        buttonTextColor = Color.parseColor("#28532A") // MaterialButton 텍스트 색상
    )

    // 특별 테마 설정
    val specialTheme = ThemeItem(
        id = "special_theme_1",
        title = "기간 한정 세일 테마!",
        description = "11월 30일까지 반값 테마!",
        imageResId = R.drawable.shop_special,
        price = 300,
        backgroundColor = Color.parseColor("#FAF3F3"),
        treeImageResId = R.drawable.tree_spring,
        hillImageResId = R.drawable.spring_hill,
        buttonIconColor = Color.parseColor("#B96464"),
        progressBarIndicatorColor = Color.parseColor("#E38989"),
        progressBarTrackColor = Color.parseColor("#FFE3E3"),
        textColor = Color.parseColor("#B96464"),
        buttonTextColor = Color.parseColor("#B96464")
    )

    // 기본 테마 리스트 설정
    val basicThemes = listOf(
        ThemeItem(
            id = "basic_theme_1",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500,
            backgroundColor = Color.parseColor("#DFF1F3"),
            treeImageResId = R.drawable.tree_winter,
            hillImageResId = R.drawable.winter_hill,
            buttonIconColor = Color.parseColor("#6D6D6D"),
            progressBarIndicatorColor = Color.parseColor("#FCFDFE"),
            progressBarTrackColor = Color.parseColor("#CBD6DC"),
            textColor = Color.parseColor("#6D6D6D"),
            buttonTextColor = Color.parseColor("#6D6D6D")
        ),
        ThemeItem(
            id = "basic_theme_2",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500,
            backgroundColor = Color.parseColor("#DFF1F3"),
            treeImageResId = R.drawable.tree_winter,
            hillImageResId = R.drawable.winter_hill,
            buttonIconColor = Color.parseColor("#6D6D6D"),
            progressBarIndicatorColor = Color.parseColor("#FCFDFE"),
            progressBarTrackColor = Color.parseColor("#CBD6DC"),
            textColor = Color.parseColor("#6D6D6D"),
            buttonTextColor = Color.parseColor("#6D6D6D")
        ),
        ThemeItem(
            id = "basic_theme_3",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500,
            backgroundColor = Color.parseColor("#DFF1F3"),
            treeImageResId = R.drawable.tree_winter,
            hillImageResId = R.drawable.winter_hill,
            buttonIconColor = Color.parseColor("#6D6D6D"),
            progressBarIndicatorColor = Color.parseColor("#FCFDFE"),
            progressBarTrackColor = Color.parseColor("#CBD6DC"),
            textColor = Color.parseColor("#6D6D6D"),
            buttonTextColor = Color.parseColor("#6D6D6D")
        ),
        ThemeItem(
            id = "basic_theme_4",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500,
            backgroundColor = Color.parseColor("#DFF1F3"),
            treeImageResId = R.drawable.tree_winter,
            hillImageResId = R.drawable.winter_hill,
            buttonIconColor = Color.parseColor("#6D6D6D"),
            progressBarIndicatorColor = Color.parseColor("#FCFDFE"),
            progressBarTrackColor = Color.parseColor("#CBD6DC"),
            textColor = Color.parseColor("#6D6D6D"),
            buttonTextColor = Color.parseColor("#6D6D6D")
        ),
        ThemeItem(
            id = "basic_theme_5",
            title = "크리스마스 한정 산타 테마!",
            description = "12/25까지 산타 테마를 즐겨보아요",
            imageResId = R.drawable.theme_winter,
            price = 500,
            backgroundColor = Color.parseColor("#DFF1F3"),
            treeImageResId = R.drawable.tree_winter,
            hillImageResId = R.drawable.winter_hill,
            buttonIconColor = Color.parseColor("#6D6D6D"),
            progressBarIndicatorColor = Color.parseColor("#FCFDFE"),
            progressBarTrackColor = Color.parseColor("#CBD6DC"),
            textColor = Color.parseColor("#6D6D6D"),
            buttonTextColor = Color.parseColor("#6D6D6D")
        )
    )

    private val allThemes = listOf(specialTheme) + basicThemes


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 프래그먼트 백스택 변경 리스너
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.viewPager.visibility = View.VISIBLE
                binding.shopFragmentContainer.visibility = View.GONE
            } else {
                binding.shopFragmentContainer.visibility = View.VISIBLE
            }
        }

        // FragmentContainer가 항상 최상위에 오도록 설정
        binding.shopFragmentContainer.elevation = 2f
        binding.viewPager.elevation = 1f

        // ViewPager 설정
        val adapter = HomeBNVAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = currentSelectedIndex

        // 테마 복원
        restoreSavedTheme()

        // BottomNavigationView 화면 전환
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedIndex = when (item.itemId) {
                R.id.menu_archive -> 0
                R.id.menu_home -> 1
                R.id.menu_mypage -> 2
                else -> return@setOnItemSelectedListener false
            }

            if (selectedIndex != currentSelectedIndex) {
                binding.viewPager.visibility = View.VISIBLE
                binding.viewPager.setCurrentItem(selectedIndex, true)
                currentSelectedIndex = selectedIndex
            }

            // FragmentContainerView 안의 프래그먼트를 제거
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            true
        }


        // ViewPager 페이지 변경 리스너 설정
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSelectedIndex = position
                binding.bottomNavigationView.menu.getItem(position).isChecked = true

                // HomeFragment가 선택되었을 경우 테마를 복원
                if (position == 1 && !isThemeRestored) {
                    restoreSavedTheme()
                    isThemeRestored = true
                }

                // FragmentContainerView 초기화
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
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


    // 홈 프래그먼트에 테마 전달
    fun applyThemeToHome(themeItem: ThemeItem) {
        Log.d("Theme", "테마 적용 시도: ${themeItem.title}")

        // 현재 활성화 여부와 관계없이 HomeFragment를 찾아 테마 적용
        val homeFragment = supportFragmentManager.fragments.find { it is HomeFragment } as? HomeFragment
        if (homeFragment != null) {
            Log.d("Theme", "HomeFragment를 찾음. 테마 적용 중...")
            homeFragment.applyTheme(themeItem)
        } else {
            Log.e("Theme", "HomeFragment를 찾을 수 없습니다. 테마 적용 실패.")
        }
    }

    fun getThemeById(id: String): ThemeItem? {
        return allThemes.find { it.id == id }
    }


    // 테마 복원 메서드
    private fun restoreSavedTheme() {
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedThemeId = sharedPrefs.getString("selected_theme", null)

        // 저장된 테마 ID로 ThemeItem 찾기
        val selectedTheme = allThemes.find { it.id == savedThemeId } ?: defaultTheme

        // ViewPager가 HomeFragment를 로드한 후에 테마 적용
        binding.viewPager.post {
            applyThemeToHome(selectedTheme)
        }
    }

    // 현재 ViewPager에서 보이는 Fragment 가져오기
    private fun getCurrentFragment(): Fragment? {
        val currentItem = binding.viewPager.currentItem
        val fragmentTag = "android:switcher:${R.id.viewPager}:$currentItem"
        return supportFragmentManager.findFragmentByTag(fragmentTag)
    }


    // 프래그먼트 전환을 위한 메서드
    fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        binding.viewPager.visibility = View.GONE
        binding.shopFragmentContainer.visibility = View.VISIBLE

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.shopFragmentContainer, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }



    fun showShopFragment() {
        val shopFragment = ShopFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down) // 애니메이션 설정
            .add(R.id.shopFragmentContainer, shopFragment)
            .addToBackStack("ShopFragment") // 백스택 추가
            .commit()

        // BottomNavigationView 숨기기
        binding.bottomNavigationView.visibility = View.GONE
        binding.shopFragmentContainer.visibility = View.VISIBLE
    }

    fun hideShopFragment() {
        supportFragmentManager.popBackStack("ShopFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE)
        binding.shopFragmentContainer.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.VISIBLE
    }



    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }


        // Fragment가 숨겨진 뒤 BottomNavigationView를 다시 보이도록 설정
        binding.shopFragmentContainer.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.VISIBLE
    }
}