package com.example.shickjip

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import com.example.shickjip.databinding.ItemInfomodalBinding

class PlantInfoDialog(
    context: Context,
    private val title: String,
    private val description: String,
    private val onButtonClick: () -> Unit // 버튼 클릭 콜백
) : Dialog(context) {

    private lateinit var binding: ItemInfomodalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ItemInfomodalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다이얼로그 스타일 설정
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 텍스트 설정
        binding.plantTitle.text = title
        binding.plantDescription.text = description

        // 버튼 설정
        binding.registerButton.setOnClickListener {
            onButtonClick() // 콜백 호출
            dismiss() // 다이얼로그 닫기
        }

        binding.closeButton.setOnClickListener {
            onButtonClick() // 콜백 호출
            dismiss() // 다이얼로그 닫기
        }
    }
}