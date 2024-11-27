package com.example.shickjip

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shickjip.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater) // 뷰 바인딩 초기화
        setContentView(binding.root)

        // 뒤로가기 버튼 클릭 리스너
        binding.backButton.setOnClickListener {
            finish() // 현재 Activity 종료
        }
    }

}