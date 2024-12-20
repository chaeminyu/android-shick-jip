package com.example.shickjip

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 현재 사용자의 정보 가져오기
        auth.currentUser?.let { user ->
            firestore.collection("users")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val username = documents.documents[0].getString("username") ?: "사용자"
                        binding.profileName.text = "${username}'s collection"
                    }
                }
        }
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

    //chatgpt 추천코드
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
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

                if (snapshot != null && !snapshot.isEmpty) {
                    Log.d("Firestore", "Fetched plants: ${snapshot.documents}")
                    plantsList.clear()
                    for (doc in snapshot.documents) {
                        Log.d("Firestore", "Plant data: ${doc.data}")
                        doc.toObject(Plant::class.java)?.let { plant ->
                            plantsList.add(plant)
                        }
                    }
                    archiveAdapter.notifyDataSetChanged()
                } else {
                    Log.d("Firestore", "No plants found for user ${currentUser.uid}")
                }
            }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
