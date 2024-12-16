package com.example.shickjip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shickjip.databinding.ItemArchiveRecyclerviewBinding

class ArchiveAdapter(private val archives:List<String>) :
    RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder>() {

    // ViewHolder class to hold individual list item views
    class ArchiveViewHolder(val binding: ItemArchiveRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val binding = ItemArchiveRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArchiveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        val binding = holder.binding
        binding.archiveImage.setImageResource(R.drawable.archive_default)

        when (archives[position]) {
            "아카이브1" -> binding.archiveImage.setImageResource(R.drawable.avatar_1)
            "아카이브2" -> binding.archiveImage.setImageResource(R.drawable.avatar_10)
            "아카이브3" -> binding.archiveImage.setImageResource(R.drawable.avatar_11)
            "아카이브4" -> binding.archiveImage.setImageResource(R.drawable.avatar_12)
            "아카이브5" -> binding.archiveImage.setImageResource(R.drawable.avatar_13)
            "아카이브6" -> binding.archiveImage.setImageResource(R.drawable.avatar_14)
            "아카이브7" -> binding.archiveImage.setImageResource(R.drawable.avatar_15)
        }

        holder.binding.archiveImage.post {
            val width = holder.binding.archiveImage.width
            holder.binding.archiveImage.layoutParams.height = width+20
            holder.binding.archiveImage.requestLayout()
        }
    }

    override fun getItemCount(): Int = archives.size
}
