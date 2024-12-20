package com.example.shickjip

import com.example.shickjip.BuildConfig
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val containerLayout = findViewById<LinearLayout>(R.id.containerLayout)
        val flashButton = findViewById<ImageButton>(R.id.flashButton)
        val captureButton = findViewById<ImageButton>(R.id.captureButton)
        val switchButton = findViewById<ImageButton>(R.id.switchCameraButton)

        containerLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = containerLayout.height  // 부모 레이아웃 높이
            val buttonSize = containerHeight * 5 / 2       // 버튼 크기를 부모 높이의 1/2로 설정

            // 버튼 크기 설정
            flashButton.layoutParams.height = containerHeight * 2 /5
            flashButton.layoutParams.width = containerHeight * 2 /5
            flashButton.requestLayout()

            captureButton.layoutParams.height = containerHeight * 4 / 5
            captureButton.layoutParams.width = containerHeight * 4 / 5
            captureButton.requestLayout()

            switchButton.layoutParams.height = containerHeight * 2 /5
            switchButton.layoutParams.width = containerHeight * 2 /5
            switchButton.requestLayout()
        }


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
            animateCaptureButton()
            takePhoto()
        }
        // 플래시 버튼 클릭 리스너
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }

        // 카메라 전환 버튼 클릭 리스너
        binding.switchCameraButton.setOnClickListener {
            toggleCamera()
        }
    }
    private fun toggleFlash() {
        flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) {
            binding.flashButton.setImageResource(R.drawable.ic_flash_on)
            ImageCapture.FLASH_MODE_ON
        } else {
            binding.flashButton.setImageResource(R.drawable.ic_flash_off)
            ImageCapture.FLASH_MODE_OFF
        }

        // 캡쳐 설정 업데이트
        imageCapture?.flashMode = flashMode
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // 카메라 재시작
        startCamera()
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

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 로딩 중 모달 표시
        showScanDialog(ModalState.LOADING) {
            val photoFile = File(
                getOutputDirectory(),
                "Plant_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        lifecycleScope.launch {
                            identifyPlantWithApi(photoFile)
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        showScanDialog(ModalState.FAILURE) {}
                    }
                }
            )
        }
    }
    private fun showPlantInfoDialog(title: String, description: String, photoFile: File) {
        val plantInfoDialog = PlantInfoDialog(
            context = this,
            title = title,
            description = description,
            imagePath = photoFile.absolutePath
        ) {
            finish() // 등록 성공 후 카메라 닫음
        }
        plantInfoDialog.show()
    }

    private suspend fun identifyPlantWithApi(photoFile: File) {
        if (!isNetworkAvailable()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CameraActivity,
                    "네트워크 연결을 확인해주세요",
                    Toast.LENGTH_SHORT).show()
                showScanDialog(ModalState.FAILURE) {}
            }
            return
        }
        try {
            // 이미지 최적화
            val optimizedFile = optimizeImage(photoFile)
            // MultipartBody.Part 생성
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), photoFile)
            val imagePart = MultipartBody.Part.createFormData("images", photoFile.name, requestFile)

            // API 호출
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
                            // Get plant details from the suggestion
                            val details = suggestion.details

                            // Create display name combining common names and scientific name
                            val commonNames = details?.common_names?.joinToString(", ") ?: ""
                            val displayName = if (commonNames.isNotEmpty()) {
                                "$commonNames (${suggestion.name})"
                            } else {
                                suggestion.name
                            }

                            // Get description from the details
                            val description = details?.description?.value
                                ?: "이 식물에 대한 설명을 찾을 수 없습니다."

                            showPlantInfoDialog(
                                title = displayName,
                                description = description,
                                photoFile = photoFile
                            )
                            val result = Intent().apply {
                                putExtra("plant_name", commonNames)
                                putExtra("plant_description", description)
                                putExtra("image_path", optimizedFile.absolutePath)
                                putExtra("captured_date", System.currentTimeMillis())
                            }
                            setResult(RESULT_OK, result)
                        }
                    } else {
                        showScanDialog(ModalState.FAILURE) {}
                    }
                }
                else {
                    showScanDialog(ModalState.FAILURE) {}
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e(TAG, "Plant identification failed", e)
                showScanDialog(ModalState.FAILURE) {}
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    // 이미지 최적화를 위함
    private fun optimizeImage(originalFile: File): File {
        // 이미지 최적화 로직
        val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
        val maxSize = 1024 // 최대 1024px

        val ratio = Math.min(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )

        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val optimizedFile = File(originalFile.parent, "optimized_${originalFile.name}")
        optimizedFile.outputStream().use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        return optimizedFile
    }

    // 카메라 권한 체크 및 카메라 시작
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

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
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
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
