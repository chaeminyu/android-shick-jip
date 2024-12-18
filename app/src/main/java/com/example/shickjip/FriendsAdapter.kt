package com.example.shickjip

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shickjip.databinding.ItemFriendsRecyclerviewBinding

class FriendsAdapter(private val friends: List<String>) :
    RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder>() {

    // ViewHolder class to hold individual list item views
    class FriendsViewHolder(val binding: ItemFriendsRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val binding = ItemFriendsRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int) {
        val binding = holder.binding
        binding.friendsProfile.setImageResource(android.R.drawable.star_on)

        when (friends[position]) {
            "친구1" -> binding.friendsProfile.setImageResource(R.drawable.avatar_1)
            "친구2" -> binding.friendsProfile.setImageResource(R.drawable.avatar_10)
            "친구3" -> binding.friendsProfile.setImageResource(R.drawable.avatar_11)
            "친구4" -> binding.friendsProfile.setImageResource(R.drawable.avatar_12)
            "친구5" -> binding.friendsProfile.setImageResource(R.drawable.avatar_13)
            "친구6" -> binding.friendsProfile.setImageResource(R.drawable.avatar_14)
            "친구7" -> binding.friendsProfile.setImageResource(R.drawable.avatar_15)
        }
    }

    override fun getItemCount(): Int = friends.size
}