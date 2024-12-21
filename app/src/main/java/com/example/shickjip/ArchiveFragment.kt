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
import androidx.viewpager2.widget.ViewPager2
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

        // 현재 로그인된 사용자의 UID 가져오기
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Firestore에서 사용자 정보 조회
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("ArchiveFragment", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val username = snapshot.getString("username") ?: "사용자"
                        binding.profileName.text = "${username}'s collection"
                    }
                }
        }

        setupRecyclerView()
        loadPlants()
    }

    private fun setupRecyclerView() {
        archiveAdapter = ArchiveAdapter(plantsList) { plantId ->
            Log.d("ArchiveFragment", "Navigating to PlantDetailFragment with plantId: $plantId")

            // ViewPager 숨기기
            requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.shopFragmentContainer, PlantDetailFragment.newInstance(plantId))
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
            // 이 부분이 인덱스가 필요한 복합 쿼리입니다
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ArchiveFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
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
