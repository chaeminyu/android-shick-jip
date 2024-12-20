package com.example.shickjip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.shickjip.databinding.FragmentPlantDetailBinding
import com.example.shickjip.models.Plant
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDetailFragment : Fragment() {
    private var _binding: FragmentPlantDetailBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    private var plant: Plant? = null
    private lateinit var plantId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            plantId = it.getString(ARG_PLANT_ID) ?: throw IllegalArgumentException("Plant ID required")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadPlantDetails()
    }

    private fun setupViews() {
        binding.backButton.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack() // Fragment를 BackStack에서 제거
        }

        binding.writeDiaryButton.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.viewPager, DiaryWritingFragment.newInstance(plantId))
                ?.addToBackStack(null)
                ?.commit()
        }
    }


    private fun loadPlantDetails() {
        firestore.collection("plants").document(plantId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("PlantDetailFragment", "Error fetching plant details", e)
                    Toast.makeText(context, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(Plant::class.java)?.let { plant ->
                        this.plant = plant
                        updateUI(plant)
                    }
                } else {
                    Toast.makeText(context, "해당 식물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun updateUI(plant: Plant) {
        binding.apply {
            plantName.text = plant.name
            plantDescription.text = plant.description
            registrationDate.text = SimpleDateFormat("yyyy년 MM월 dd일 등록", Locale.getDefault())
                .format(Date(plant.registrationDate))

            Glide.with(requireContext())
                .load(plant.imagePath)
                .into(plantImage)

//            // 식물 일기가 있다면 업데이트
//            diaryEntriesLayout.removeAllViews()
//            plant.diaryEntries.forEach { entry ->
//                // Add diary entry views
//                val entryView = layoutInflater.inflate(R.layout.item_diary_entry, diaryEntriesLayout, false)
//                entryView.findViewById<TextView>(R.id.entryContent).text = entry.content
//                entryView.findViewById<TextView>(R.id.entryDate).text =
//                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(entry.date))
//
//                diaryEntriesLayout.addView(entryView)
//            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
    }



    companion object {
        private const val ARG_PLANT_ID = "plant_id"

        fun newInstance(plantId: String): PlantDetailFragment {
            return PlantDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLANT_ID, plantId) // plantId를 번들에 저장
                }
            }
        }
    }
}