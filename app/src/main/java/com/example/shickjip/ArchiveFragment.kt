package com.example.shickjip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shickjip.databinding.FragmentArchiveBinding

class ArchiveFragment : Fragment() {
    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)

        // set layout manager - linear
        val linearLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        // Setting up RecyclerView - friends list
        val friends = mutableListOf("친구1", "친구2", "친구3", "친구4", "친구5", "친구6", "친구7")
        binding.friendsList.layoutManager = linearLayoutManager
        val friendsAdapter = FriendsAdapter(friends)
        binding.friendsList.adapter = friendsAdapter

        //set layout manager - grid
        var gridLayoutManager = GridLayoutManager(activity, 2)
        // Setting up RecyclerView - archive list
        val archive = mutableListOf("아카이브1", "아카이브2", "아카이브3", "아카이브4", "아카이브5", "아카이브6", "아카이브7")
        binding.archiveList.layoutManager = gridLayoutManager
        val archiveAdapter = ArchiveAdapter(archive)
        binding.archiveList.adapter = archiveAdapter

        return binding.root
    }
}
