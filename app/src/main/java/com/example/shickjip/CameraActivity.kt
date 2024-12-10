package com.example.shickjip


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shickjip.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼 클릭 리스너
        binding.backButton.setOnClickListener {
            finish() // 현재 Activity 종료
        }

        binding.captureButton.setOnClickListener {
            animateCaptureButton()

            // 로딩 중 모달 표시
            showScanDialog(ModalState.LOADING) {
                // 랜덤으로 성공/실패 결정
                val isSuccess = (0..1).random() == 1
                if (isSuccess) {
                    showScanDialog(ModalState.SUCCESS) {
                        // 성공 시: 식물 정보 모달 표시
                        showPlantInfoDialog(
                            title = "민들레속",
                            description = "민들레속은 국화과에 속하는 식물로, 노란색 꽃이 통꽃 형태로 100개에서 200개가 모여 한 송이를 이룹니다. 이 작은 꽃들은 '두상화'라고 불리며, 씨앗은 바람에 날아가 땅에 떨어져 자랍니다. 민들레의 줄기를 자르면 쓴 맛이 나는 하얀 즙이 나오며, 유럽에서는 식용으로 사용됩니다. 한국에서는 주로 잎을 김치나 나물, 샐러드에 활용하고, 뿌리를 말려 커피 대용으로도 씁니다. 또한, 민들레는 어린이들의 좋은 장난감이 되기도 하는데, 꽃대를 잘라 비누방울을 불거나, 피리를 만들어 놀 수 있습니다."
                        )
                    }
                } else {
                    showScanDialog(ModalState.FAILURE) {
                        // 실패 시: 아무 작업도 하지 않음
                    }
                }
            }
        }
    }

    private fun showScanDialog(state: ModalState, onDismiss: (() -> Unit)?) {
        val dialog = ScanDialog(state, onDismiss)
        dialog.isCancelable = false // 다이얼로그 외부 클릭 방지
        dialog.show(supportFragmentManager, "ScanDialog")
    }

    private fun showPlantInfoDialog(title: String, description: String) {
        val plantInfoDialog = PlantInfoDialog(
            context = this,
            title = title,
            description = description
        ) {
            finish() // 버튼 클릭 시 현재 액티비티 종료
        }
        plantInfoDialog.show()
    }

    private fun animateCaptureButton() {
        binding.captureButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.captureButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }


}
