package com.example.shickjip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BannerAdapter(private val banners: List<Banner>) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val banner = banners[position]
        holder.bind(banner)
    }

    override fun getItemCount(): Int = banners.size

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bannerImage: ImageView = itemView.findViewById(R.id.banner_image)
        private val bannerTitle: TextView = itemView.findViewById(R.id.banner_title)
        private val bannerDescription: TextView = itemView.findViewById(R.id.banner_description)

        fun bind(banner: Banner) { // 배너 요소들 설정
            bannerImage.setImageResource(banner.imageResId)
            bannerTitle.text = banner.title
            bannerDescription.text = banner.description
        }
    }
}

data class Banner(
    val imageResId: Int,
    val title: String,
    val description: String
)