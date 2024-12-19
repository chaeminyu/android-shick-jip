package com.example.shickjip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shickjip.databinding.ItemThemeBinding

data class ThemeItem(
    val title: String,
    val description: String,
    val imageResId: Int,
    val price: Int
)

class ThemeAdapter(private val themeList: List<ThemeItem>) :
    RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themeList[position]
        holder.bind(theme)
    }

    override fun getItemCount(): Int = themeList.size

    class ThemeViewHolder(private val binding: ItemThemeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(theme: ThemeItem) {
            binding.title.text = theme.title
            binding.description.text = theme.description
            binding.themeImage.setImageResource(theme.imageResId)
            binding.price.text = theme.price.toString()

            binding.btnPurchase.setOnClickListener {
                // 결제 버튼 클릭 시 수행할 동작
                println("${theme.title} 결제 버튼이 클릭되었습니다.")
            }
        }
    }
}