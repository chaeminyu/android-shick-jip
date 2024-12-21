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

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class ArchiveAdapter(
    private val plants: List<Plant>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder>() {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

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
        holder.itemView.setOnClickListener {
            Log.d("ArchiveAdapter", "Item clicked: ${plant.id}")
            onItemClick(plant.id)
        }

        holder.binding.apply {
            nickname.text = plant.nickname.ifEmpty {
                val defaultNickname = "식물 ${position + 1}" // position 기반
                plant.nickname = defaultNickname
                defaultNickname
            }
            officialName.text = plant.name
            registrationDate.text = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                .format(Date(plant.registrationDate))

            // Firebase Storage에서 이미지 다운로드 URL 가져오기
            if (!plant.imagePath.isNullOrEmpty()) {
                try {
                    // gs:// 경로를 Reference로 변환
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(plant.imagePath)

                    // Firebase Storage에서 다운로드 URL 가져오기
                    storageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.d("ArchiveAdapter", "Download URL: $uri")
                            // URI를 Glide로 로드
                            Glide.with(holder.binding.root.context)
                                .load(uri) // 다운로드 URL
                                .placeholder(R.drawable.archive_default) // 기본 이미지
                                .error(R.drawable.error_image) // 오류 시 표시할 이미지
                                .into(holder.binding.archiveImage) // 이미지 뷰에 로드
                        }
                        .addOnFailureListener { exception ->
                            Log.e("ArchiveAdapter", "Failed to get download URL: ${exception.message}", exception)
                            // 오류 시 기본 이미지 설정
                            Glide.with(holder.binding.root.context)
                                .load(R.drawable.icon_plant)
                                .into(holder.binding.archiveImage)
                        }
                } catch (e: IllegalArgumentException) {
                    Log.e("ArchiveAdapter", "Invalid Storage URL: ${plant.imagePath}", e)
                    // 기본 이미지 설정
                    Glide.with(holder.binding.root.context)
                        .load(R.drawable.ic_plant)
                        .into(holder.binding.archiveImage)
                }
            } else {
                Log.w("ArchiveAdapter", "Image path is empty or null for plant: ${plant.id}")
                Glide.with(holder.binding.root.context)
                    .load(R.drawable.archive_default)
                    .into(holder.binding.archiveImage)
            }
        }
    }

    override fun getItemCount() = plants.size
}