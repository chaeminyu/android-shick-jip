package com.example.shickjip

import ThemeItem
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shickjip.databinding.ItemThemeBinding
import com.google.android.material.button.MaterialButton

class ThemeAdapter(
    private val themes: List<ThemeItem>,
    private val onPaymentClick: (ThemeItem) -> Unit,
    private val onApplyClick: (ThemeItem) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        holder.bind(themes[position])
    }

    override fun getItemCount(): Int = themes.size

    inner class ThemeViewHolder(private val binding: ItemThemeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(theme: ThemeItem) {
            binding.title.text = theme.title
            binding.description.text = theme.description
            binding.themeImage.setImageResource(theme.imageResId)
            binding.price.text = "${theme.price}"

            val button = binding.btnPurchase as MaterialButton

            if (theme.isPurchased) {
                // 적용하기 버튼 설정
                button.text = "적용하기"
                button.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.green5)
                )
                button.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
                button.strokeColor = ContextCompat.getColorStateList(binding.root.context, R.color.green5) // 테두리 색상 변경

                button.setOnClickListener {
                    onApplyClick(theme)
                }
            } else {
                // 결제하기 버튼 설정
                button.text = "결제하기"
                button.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
                button.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.green8)
                )
                button.strokeColor = ContextCompat.getColorStateList(binding.root.context, R.color.green7) // 테두리 색상 설정

                button.setOnClickListener {
                    onPaymentClick(theme)
                }
            }
        }
    }
}