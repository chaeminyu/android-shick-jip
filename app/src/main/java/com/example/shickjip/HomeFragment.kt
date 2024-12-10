package com.example.shickjip

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.CycleInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.shickjip.databinding.FragmentHomeBinding
import me.relex.circleindicator.CircleIndicator3

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var handler: Handler
    private lateinit var bannerAdapter: BannerAdapter

    private var experience = 1980 // 현재 경험치
    private val levels = listOf(0, 500, 1000, 1500, 2000) // 레벨 범위
    private val levelNames = listOf("초보", "루키", "고수", "전문가", "그 자체") // 레벨 이름
    private val levelColors = listOf("#9F8731", "#F2C50E", "#24a8e0", "#9f1fd1", "#4DC03B") // 레벨별 색상

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        // 초기 상태에서 툴팁 숨김
        binding.tooltipContainer.visibility = View.GONE

        // ProgressBar 클릭 시 툴팁 표시/숨김 (페이드 효과 포함)
        binding.progressBar.setOnClickListener {
            if (binding.tooltipContainer.visibility == View.GONE) {
                fadeIn(binding.tooltipContainer)
            } else {
                fadeOut(binding.tooltipContainer)
            }
        }

        // 메인 레이아웃 클릭 시 툴팁 숨김 (페이드 효과 포함)
        binding.main.setOnClickListener {
            if (binding.tooltipContainer.visibility == View.VISIBLE) {
                fadeOut(binding.tooltipContainer)
            }
        }

        // 툴팁 자체 클릭 방지
        binding.tooltipContainer.setOnClickListener {
            // Do nothing
        }


        return view
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
        binding.viewPager.adapter = bannerAdapter

        // 인디케이터 설정
        val indicator: CircleIndicator3 = binding.indicator
        indicator.setViewPager(binding.viewPager)

        // 자동 슬라이드 시작
        handler = Handler(Looper.getMainLooper())
        startAutoSlide(banners.size)

        // 버튼 클릭 시 CameraActivity로 이동
        binding.buttonWithIcon.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }
        // 경험치로 현재 레벨 계산
        val currentLevelIndex = calculateLevel(experience)
        val currentLevel = levelNames[currentLevelIndex]
        val nextLevel = if (currentLevelIndex + 1 < levels.size) levelNames[currentLevelIndex + 1] else "최고 레벨"

        val currentLevelStartExp = levels[currentLevelIndex]
        val nextLevelStartExp = levels.getOrNull(currentLevelIndex + 1) ?: Int.MAX_VALUE

        val currentLevelColor = levelColors[currentLevelIndex]
        val nextLevelColor = if (currentLevelIndex + 1 < levelColors.size) levelColors[currentLevelIndex + 1] else "#FFD700"

        // 현재 레벨 내 경험치 및 최대 경험치 계산
        val currentExpInLevel = experience - currentLevelStartExp
        val maxExpInLevel = if (currentLevelIndex + 1 < levels.size) {
            nextLevelStartExp - currentLevelStartExp
        } else {
            // 마지막 레벨에서 이전 구간의 범위를 사용
            levels.last() - levels[levels.size - 2]
        }

        // 퍼센트 값 계산 (초과 경험치도 반영)
        val progressPercent = if (currentLevelIndex == levels.size - 1) {
            // 최대 레벨일 경우 초과 퍼센트 계산
            (currentExpInLevel.toFloat() / maxExpInLevel) * 100
        } else {
            (currentExpInLevel.toFloat() / maxExpInLevel) * 100
        }

        // ProgressBar 업데이트 (100% 이상 표시 가능)
        binding.progressBar.progress = progressPercent.toInt()

        // 레벨 상태에 따른 뷰 설정
        if (currentLevelIndex == levels.size - 1) {
            // 최대 레벨일 경우
            binding.progressBar.visibility = View.INVISIBLE
            binding.sunIcon.visibility = View.VISIBLE

            // sunIcon 클릭 리스너 설정 (최고 레벨 상태)
            binding.sunIcon.setOnClickListener {
                if (binding.tooltipContainer.visibility == View.GONE) {
                    fadeIn(binding.tooltipContainer)
                } else {
                    fadeOut(binding.tooltipContainer)
                }
            }
        } else {
            // 최대 레벨이 아닐 경우
            binding.progressBar.visibility = View.VISIBLE
            binding.sunIcon.visibility = View.GONE
            binding.progressBar.progress = progressPercent.toInt().coerceAtMost(100)

            // ProgressBar 클릭 리스너 설정
            binding.progressBar.setOnClickListener {
                if (binding.tooltipContainer.visibility == View.GONE) {
                    fadeIn(binding.tooltipContainer)
                } else {
                    fadeOut(binding.tooltipContainer)
                }
            }
        }
        // TextView 업데이트 (퍼센트 출력 형식 변경)
        val epsilon = 0.0001f
        val progressText = if (progressPercent % 1 < epsilon || 1 - (progressPercent % 1) < epsilon) {
            "${currentLevel}\n${progressPercent.toInt()}%"
        } else {
            "${currentLevel}\n${String.format("%.1f", progressPercent)}%"
        }
        binding.title.text = progressText

        // 안내 메시지 업데이트
        val isMaxLevel = currentLevelIndex == levels.size - 1
        val username = "수진" // 예시 사용자 이름
        updateTitleText(username, currentLevel, currentLevelColor)
        updateTooltipText(nextLevel, nextLevelColor, isMaxLevel, currentExpInLevel)


        binding.homeTree.setOnTouchListener { _, _ ->
            // Pivot 설정: 아래가 고정
            binding.homeTree.apply {
                pivotX = width / 2f // 가로 중앙
                pivotY = height.toFloat() // 세로 하단 고정
            }

            // 흔들기 애니메이션 실행
            swayTree(binding.homeTree)
            true
        }

    }

    private fun swayTree(treeView: View) {

        treeView.apply {
            pivotX = width / 2f // 가로 중심
            pivotY = height.toFloat() // 세로 하단
        }

        val translationAnimator = ObjectAnimator.ofFloat(treeView, "translationX", 0f, -10f, 10f, -5f, 5f, 0f).apply {
            duration = 2500 // 2초 동안 이동
            interpolator = AccelerateDecelerateInterpolator() // 부드러운 리듬
        }

        val rotationAnimator = ObjectAnimator.ofFloat(treeView, "rotation", 0f, -2f, 2f, 0.6f, 0.6f, 0f).apply {
            duration = 2500
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(translationAnimator, rotationAnimator)
            start()
        }
    }


    private fun startAutoSlide(itemCount: Int) {
        val runnable = object : Runnable {
            override fun run() {
                val nextItem = (binding.viewPager.currentItem + 1) % itemCount
                binding.viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 4000) // 4초 간격으로 슬라이드
            }
        }
        handler.postDelayed(runnable, 4000)
    }

    // 페이드 인 애니메이션
    private fun fadeIn(view: View) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null)
        }
    }

    // 페이드 아웃 애니메이션
    private fun fadeOut(view: View) {
        view.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                view.visibility = View.GONE
            }
    }

    private fun calculateLevel(exp: Int): Int {
        for (i in levels.indices) {
            if (exp < levels[i]) return i - 1
        }
        return levels.size - 1
    }

    private fun updateTitleText(username: String, level: String, levelColor: String) {
        val spannable = SpannableStringBuilder()

        // 커스텀 글꼴 가져오기
        val customFont = context?.let { ResourcesCompat.getFont(it, R.font.pretendard_extrabold) } ?: Typeface.DEFAULT

        // 텍스트 구성
        spannable.append("${username}님은 식물 ")

        val levelStart = spannable.length
        spannable.append(level)

        spannable.append("네요!")

        // 커스텀 글꼴 적용
        spannable.setSpan(
            CustomTypefaceSpan(customFont),
            levelStart,
            levelStart + level.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 색상 적용
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor(levelColor)),
            levelStart,
            levelStart + level.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tooltipTitle.text = spannable
    }

    private fun updateTooltipText(nextLevel: String, nextLevelColor: String, isMaxLevel: Boolean, point: Int) {
        val spannable = SpannableStringBuilder()

        // Pretendard Medium 글꼴 가져오기
        val pretendardMedium = context?.let { ResourcesCompat.getFont(it, R.font.pretendard_bold) }
            ?: Typeface.DEFAULT

        if (isMaxLevel) {
            // 최대 레벨일 경우의 메시지
            spannable.append("이미 최고 레벨인데도 ")

            // ${point}pt 추가 및 시작 위치 저장
            val pointStart = spannable.length
            spannable.append("${point} 포인트")

            // Pretendard Medium 적용
            spannable.setSpan(
                CustomTypefaceSpan(pretendardMedium),
                pointStart,
                pointStart + "${point} 포인트".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 색상 적용
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#358437")),
                pointStart,
                pointStart + "${point} 포인트".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.append("나 더 모으셨네요.\n포인트를 계속 모아볼까요? 정원이 더 특별해질 기회가 올지도 몰라요!")
        } else {
            // 최대 레벨이 아닐 경우의 메시지
            spannable.append("더 많은 포인트를 모아서 ")

            // nextLevel 텍스트 추가 및 시작 위치 저장
            val nextLevelStart = spannable.length
            spannable.append(nextLevel)

            // Pretendard Medium 적용
            spannable.setSpan(
                CustomTypefaceSpan(pretendardMedium),
                nextLevelStart,
                nextLevelStart + nextLevel.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 색상 적용
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor(nextLevelColor)),
                nextLevelStart,
                nextLevelStart + nextLevel.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannable.append(" 레벨로 올라갈 수 있어요.\n식물을 찾으러 떠나거나, 일기를 작성하여 등급을 올려보세요!")
        }

        binding.tooltipBody.text = spannable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // 자동 슬라이드 중지
        _binding = null
    }
}