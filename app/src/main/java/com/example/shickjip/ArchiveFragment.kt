package com.example.shickjip

import FriendsAdapter
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.shickjip.databinding.FragmentArchiveBinding
import com.example.shickjip.models.Friend
import com.example.shickjip.models.Plant
import com.example.shickjip.ui.FriendAddFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ArchiveFragment : Fragment() {
    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 기존 기능: 식물 데이터
    private lateinit var archiveAdapter: ArchiveAdapter
    private val plantsList = mutableListOf<Plant>()

    // 추가된 기능: 친구 데이터
    private lateinit var friendAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()

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
        val currentUser = auth.currentUser

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

        // RecyclerView 초기화
        setupPlantRecyclerView()
        setupFriendRecyclerView()

        // Firestore 데이터 로드
        loadPlants()
        loadFriends()

        // 친구 추가 버튼 클릭 이벤트
        binding.addFriendButton.setOnClickListener {
            val fragment = FriendAddFragment()
            fragment.show(parentFragmentManager, "FriendAddFragment")
            Log.d("ArchiveFragment", "+ 버튼 클릭됨: 모달 표시")
        }
    }

    private fun setupPlantRecyclerView() {
        archiveAdapter = ArchiveAdapter(plantsList) { plantId ->
            Log.d("ArchiveFragment", "Navigating to PlantDetailFragment with plantId: $plantId")

            // ViewPager 숨기기
            requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE

            // shopFragmentContainer를 먼저 보이게 설정
            requireActivity().findViewById<FrameLayout>(R.id.shopFragmentContainer).visibility = View.VISIBLE

            // Fragment 전환
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

    private fun setupFriendRecyclerView() {
        friendAdapter = FriendsAdapter(friendsList) { friend ->
            Toast.makeText(context, "Clicked: ${friend.name}", Toast.LENGTH_SHORT).show()
        }
        binding.friendsList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = friendAdapter
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

    private fun loadFriends() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ArchiveFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val friendEmails = snapshot?.get("friends") as? List<String> ?: emptyList()
                if (friendEmails.isEmpty()) {
                    Log.d("ArchiveFragment", "No friends found.")
                    return@addSnapshotListener
                }

                // 친구 상세 정보 가져오기
                fetchFriendDetails(friendEmails)
            }
    }

    private fun fetchFriendDetails(friendEmails: List<String>) {
        firestore.collection("users")
            .whereIn("email", friendEmails)
            .get()
            .addOnSuccessListener { result ->
                friendsList.clear()
                for (document in result) {
                    val friend = document.toObject(Friend::class.java)
                    friendsList.add(friend)
                }
                friendAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ArchiveFragment", "Error fetching friend details: ${e.message}")
            }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}