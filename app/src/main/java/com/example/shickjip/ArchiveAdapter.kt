package com.example.shickjip

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shickjip.databinding.ItemArchiveRecyclerviewBinding
import com.example.shickjip.models.Plant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveAdapter(
    private val plants: List<Plant>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder>() {

    class ArchiveViewHolder(val binding: ItemArchiveRecyclerviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val binding = ItemArchiveRecyclerviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArchiveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        val plant = plants[position]
        holder.itemView.setOnClickListener { onItemClick(plant.id) }

        holder.binding.apply {
            officialName.text = plant.name
            registrationDate.text = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                .format(Date(plant.registrationDate))

            // Glide 써서 이미지 로딩
            Glide.with(root.context)
                .load(plant.imagePath)
                .placeholder(R.drawable.archive_default)
                .error(R.drawable.error_image) // 오류 시 표시할 이미지 추가
                .into(archiveImage)
            Log.d("ArchiveAdapter", "Loading image: ${plant.imagePath}")



        }

    }

    override fun getItemCount() = plants.size
}
