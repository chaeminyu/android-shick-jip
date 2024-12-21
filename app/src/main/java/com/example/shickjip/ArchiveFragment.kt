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

    private lateinit var archiveAdapter: ArchiveAdapter
    private val plantsList = mutableListOf<Plant>()

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
                        binding.myName.text = username
                    }
                }
        }

        // RecyclerView 초기화
        setupPlantRecyclerView()
        setupFriendRecyclerView()

        // Firestore 데이터 로드
        loadPlants() // 기본적으로 본인의 데이터를 로드
        loadFriends() // 친구 목록을 로드

        // 친구 추가 버튼 클릭 이벤트
        binding.addFriendButton.setOnClickListener {
            val fragment = FriendAddFragment()
            fragment.show(parentFragmentManager, "FriendAddFragment")
            Log.d("ArchiveFragment", "+ 버튼 클릭됨: 모달 표시")
        }

        // 내 프로필 클릭 시 자신의 식물 로드
        binding.btnProfilePicture.setOnClickListener {
            loadPlants() // 내 프로필 클릭 시 자신의 데이터를 다시 로드
            Log.d("ArchiveFragment", "내 프로필 클릭됨: 내 식물 데이터 로드")

            // 친구 목록에서 선택된 배경 초기화
            friendAdapter.resetSelection()
        }
    }

    private fun setupPlantRecyclerView() {
        // 식물 RecyclerView 어댑터 설정
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
        // 친구 RecyclerView 어댑터 설정
        friendAdapter = FriendsAdapter(friendsList) { friend ->
            // 친구 프로필 클릭 시 해당 친구의 데이터를 로드하여 archiveList 업데이트
            loadFriendPlants(friend.userId)
            Toast.makeText(context, "${friend.name}의 데이터를 불러옵니다.", Toast.LENGTH_SHORT).show()
        }
        binding.friendsList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = friendAdapter
        }
    }

    private fun updateArchiveList(plants: List<Plant>) {
        // 가져온 식물 데이터를 RecyclerView에 업데이트
        plantsList.clear()
        plantsList.addAll(plants)
        archiveAdapter.notifyDataSetChanged()
    }

    private fun loadPlants() {
        // 현재 사용자의 식물 데이터를 Firestore에서 가져오기
        val currentUser = auth.currentUser ?: return

        firestore.collection("plants")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("registrationDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val ownPlants = mutableListOf<Plant>()
                for (doc in snapshot.documents) {
                    doc.toObject(Plant::class.java)?.let { plant ->
                        ownPlants.add(plant)
                    }
                }
                updateArchiveList(ownPlants) // 본인의 데이터만 표시
            }
            .addOnFailureListener { e ->
                Log.e("ArchiveFragment", "Error fetching own plants: ${e.message}")
            }
    }

    private fun loadFriendPlants(friendUserId: String) {
        // Firestore에서 특정 친구의 식물 데이터를 가져오기
        firestore.collection("plants")
            .whereEqualTo("userId", friendUserId)
            .orderBy("registrationDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val friendPlants = mutableListOf<Plant>()
                for (doc in snapshot.documents) {
                    doc.toObject(Plant::class.java)?.let { plant ->
                        friendPlants.add(plant)
                    }
                }
                updateArchiveList(friendPlants) // 친구의 데이터로 대체
            }
            .addOnFailureListener { e ->
                Log.e("ArchiveFragment", "Error fetching friend plants: ${e.message}")
            }
    }

    private fun loadFriends() {
        // Firestore에서 친구 데이터 가져오기
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
                    val friend = document.toObject(Friend::class.java).apply {
                        name = document.getString("username") ?: "Unknown User"
                        userId = document.id
                    }
                    friendsList.add(friend)
                }
                friendAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ArchiveFragment", "Error fetching friend details: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}