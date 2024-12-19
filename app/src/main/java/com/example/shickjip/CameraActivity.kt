package com.example.shickjip

import com.example.shickjip.BuildConfig
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.shickjip.api.RetrofitClient
import com.example.shickjip.databinding.ActivityCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: Executor
    private var cameraProvider: ProcessCameraProvider? = null
    private var isProcessing = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = ContextCompat.getMainExecutor(this)

        // 카메라 권한 체크 및 요청
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 뒤로가기 버튼 클릭 리스너
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.captureButton.setOnClickListener {
            if (!isProcessing) {
                animateCaptureButton()
                takePhoto()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "카메라 권한이 필요합니다.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, cameraExecutor)
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        isProcessing = true

        // 카메라 미리보기 중지
        stopCamera()

        // 로딩 모달 표시
        showScanDialog(ModalState.LOADING) {
            val photoFile = File(
                getOutputDirectory(),
                "Plant_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        lifecycleScope.launch {
                            identifyPlantWithApi(photoFile)
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        isProcessing = false
                        showScanDialog(ModalState.FAILURE) {
                            // 실패 시 카메라 재시작
                            startCamera()
                        }
                    }
                }
            )
        }
    }

    private suspend fun identifyPlantWithApi(photoFile: File) {
        try {
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), photoFile)
            val imagePart = MultipartBody.Part.createFormData("images", photoFile.name, requestFile)

            val response = RetrofitClient.plantApiService.identifyPlant(
                apiKey = BuildConfig.PLANT_API_KEY,
                image = imagePart
            )

            withContext(Dispatchers.Main) {
                if (response.isSuccessful && response.body() != null) {
                    val plantResponse = response.body()!!
                    val suggestion = plantResponse.result.classification.suggestions.firstOrNull()

                    if (suggestion != null) {
                        showScanDialog(ModalState.SUCCESS) {
                            val description = suggestion.details?.description?.value
                                ?: "이 식물에 대한 설명을 찾을 수 없습니다."

                            showPlantInfoDialog(
                                title = suggestion.name,
                                description = description
                            )
                        }
                    } else {
                        showScanDialog(ModalState.FAILURE) {
                            startCamera() // 실패 시 카메라 재시작
                        }
                    }
                } else {
                    showScanDialog(ModalState.FAILURE) {
                        startCamera() // 실패 시 카메라 재시작
                    }
                }
                isProcessing = false
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isProcessing = false
                showScanDialog(ModalState.FAILURE) {
                    startCamera() // 실패 시 카메라 재시작
                }
            }
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
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
