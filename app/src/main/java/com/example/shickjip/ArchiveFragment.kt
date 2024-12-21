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

        // RecyclerView 초기화
        setupPlantRecyclerView()
        setupFriendRecyclerView()

        // 사용자 정보 로드 및 UI 업데이트
        loadUserInfo()

        // Firestore 데이터 로드
        loadPlants() // 기본적으로 본인의 데이터를 로드
        loadFriends() // 친구 목록 로드

        // 친구 추가 버튼 클릭 이벤트
        binding.addFriendButton.setOnClickListener {
            val fragment = FriendAddFragment()
            fragment.show(parentFragmentManager, "FriendAddFragment")
            Log.d("ArchiveFragment", "친구 추가 버튼 클릭됨: 모달 표시")
        }

        // 내 프로필 클릭 이벤트
        binding.btnProfilePicture.setOnClickListener {
            loadPlants() // 내 데이터 로드
            updateProfileName("My Collection") // 프로필 이름 업데이트
            friendAdapter.resetSelection() // 친구 선택 초기화
            Log.d("ArchiveFragment", "내 프로필 클릭됨")
        }
    }

    private fun setupPlantRecyclerView() {
        archiveAdapter = ArchiveAdapter(plantsList) { plantId ->
            val detailFragment = PlantDetailFragment.newInstance(plantId)
            (activity as? HomeActivity)?.navigateToFragment(detailFragment)
        }

        binding.archiveList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = archiveAdapter
        }
    }

    private fun setupFriendRecyclerView() {
        friendAdapter = FriendsAdapter(friendsList) { friend ->
            loadFriendPlants(friend.userId)
            updateProfileName("${friend.name}'s Collection")
            Log.d("ArchiveFragment", "${friend.name}의 데이터를 로드 중...")
        }

        binding.friendsList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = friendAdapter
        }
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser ?: return
        firestore.collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ArchiveFragment", "사용자 정보 로드 실패", e)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val username = it.getString("username") ?: "사용자"
                    binding.myName.text = username
                    updateProfileName("My Collection")
                }
            }
    }

    private fun updateProfileName(name: String) {
        binding.profileName.text = name
    }

    private fun updateArchiveList(plants: List<Plant>) {
        plantsList.clear()
        plantsList.addAll(plants)
        archiveAdapter.notifyDataSetChanged()
    }

    private fun loadPlants() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("plants")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("registrationDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val ownPlants = snapshot.documents.mapNotNull { it.toObject(Plant::class.java) }
                updateArchiveList(ownPlants)
            }
            .addOnFailureListener { e ->
                Log.e("ArchiveFragment", "식물 데이터 로드 실패: ${e.message}")
            }
    }

    private fun loadFriendPlants(friendUserId: String) {
        firestore.collection("plants")
            .whereEqualTo("userId", friendUserId)
            .orderBy("registrationDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val friendPlants = snapshot.documents.mapNotNull { it.toObject(Plant::class.java) }
                updateArchiveList(friendPlants)
            }
            .addOnFailureListener { e ->
                Log.e("ArchiveFragment", "친구 데이터 로드 실패: ${e.message}")
            }
    }

    private fun loadFriends() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ArchiveFragment", "친구 정보 로드 실패", e)
                    return@addSnapshotListener
                }

                val friendEmails = snapshot?.get("friends") as? List<String> ?: emptyList()
                if (friendEmails.isNotEmpty()) {
                    fetchFriendDetails(friendEmails)
                } else {
                    Log.d("ArchiveFragment", "등록된 친구가 없습니다.")
                }
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
                Log.e("ArchiveFragment", "친구 정보 로드 실패: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}