package com.example.shickjip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shickjip.databinding.FragmentArchiveBinding
import com.example.shickjip.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ArchiveFragment : Fragment() {
    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var archiveAdapter: ArchiveAdapter
    private val plantsList = mutableListOf<Plant>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

//        // set layout manager - linear
//        val linearLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
//        // Setting up RecyclerView - friends list
//        val friends = mutableListOf("친구1", "친구2", "친구3", "친구4", "친구5", "친구6", "친구7")
//        binding.friendsList.layoutManager = linearLayoutManager
//        val friendsAdapter = FriendsAdapter(friends)
//        binding.friendsList.adapter = friendsAdapter
//
//        //set layout manager - grid
//        var gridLayoutManager = GridLayoutManager(activity, 2)
//        // Setting up RecyclerView - archive list
//        val archive = mutableListOf("아카이브1", "아카이브2", "아카이브3", "아카이브4", "아카이브5", "아카이브6", "아카이브7")
//        binding.archiveList.layoutManager = gridLayoutManager
//        val archiveAdapter = ArchiveAdapter(archive)
//        binding.archiveList.adapter = archiveAdapter
//
//        return binding.root
//    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadPlants()
    }

    private fun setupRecyclerView() {
        archiveAdapter = ArchiveAdapter(plantsList) { plantId ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PlantDetailFragment.newInstance(plantId))
                .addToBackStack(null)
                .commit()
        }
        binding.archiveList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = archiveAdapter
        }
    }

    private fun loadPlants() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("plants")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("registrationDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ArchiveFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    plantsList.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(Plant::class.java)?.let { plant ->
                            plantsList.add(plant)
                        }
                    }
                    archiveAdapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
