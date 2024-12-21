package com.example.shickjip

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.shickjip.api.RetrofitClient
import com.example.shickjip.api.translate.RetrofitTranslate
import com.example.shickjip.databinding.ActivityCameraBinding
import com.example.shickjip.models.Plant
import com.google.android.gms.tasks.Task
import com.google.android.material.animation.AnimatorSetCompat.playTogether
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraProvider: ProcessCameraProvider

    private var isCameraActive = false // 카메라 활성 상태 추적
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo
    private var isFlashOn = false // 플래시 상태를 추적


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.containerLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = binding.containerLayout.height  // 부모 레이아웃 높이

            // 버튼 크기 설정
            binding.flashButton.layoutParams.height = containerHeight * 2 / 5
            binding.flashButton.layoutParams.width = containerHeight * 2 / 5
            binding.flashButton.requestLayout()

            binding.captureButton.layoutParams.height = containerHeight * 4 / 5
            binding.captureButton.layoutParams.width = containerHeight * 4 / 5
            binding.captureButton.requestLayout()

            binding.switchCameraButton.layoutParams.height = containerHeight * 2 / 5
            binding.switchCameraButton.layoutParams.width = containerHeight * 2 / 5
            binding.switchCameraButton.requestLayout()
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
            Log.d("Debug2", "captureButton clicked")
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

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            startCamera()
        }, ContextCompat.getMainExecutor(this))

    }

    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        if (isFlashOn) {
            binding.flashButton.setImageResource(R.drawable.ic_flash_on)
        } else {
            binding.flashButton.setImageResource(R.drawable.ic_flash_off)
        }
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
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
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
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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

        val photoFile = File(
            getOutputDirectory(),
            "Plant_${
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(System.currentTimeMillis())
            }.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // 플래시 사용 설정
        if (isFlashOn && ::cameraControl.isInitialized) {
            cameraControl.enableTorch(true) // 플래시 켜기
        }

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraActivity", "사진 저장 성공: $savedUri")

                    // 촬영 후 플래시 끄기
                    if (isFlashOn && ::cameraControl.isInitialized) {
                        cameraControl.enableTorch(false) // 플래시 끄기
                    }

                    // 로딩 모달 표시 및 식물 인식 시작
                    lifecycleScope.launch {
                        showScanOverlay(ModalState.LOADING)
                        identifyPlantWithApi(photoFile)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "사진 저장 실패", exc)
                    Toast.makeText(
                        this@CameraActivity,
                        "사진 저장 실패: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }


    private suspend fun translatePlantName(name: String, targetLang: String): String {
        // 정규식으로 일반명과 학명 분리
        val pattern = Regex("^(.*)\\((.*)\\)$")
        val matchResult = pattern.find(name)

        return if (matchResult != null) {
            val commonNamesPart = matchResult.groupValues[1].trim()
            val scientificName = matchResult.groupValues[2].trim()

            // 일반명을 쉼표로 나누고 처음 두 개만 선택
            val commonNames = commonNamesPart.split(",").map { it.trim() }.take(2)

            // 두 개의 일반명만 번역
            val translatedCommonNames = commonNames.map { translateText(it, targetLang) }

            // 번역된 일반명과 학명을 결합
            "${translatedCommonNames.joinToString(", ")} ($scientificName)"
        } else {
            // 괄호가 없는 경우, 전체를 번역
            translateText(name, targetLang)
        }
    }

    private suspend fun translateText(text: String, targetLang: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitTranslate.apiService.translate(text, targetLang).execute()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.translations.firstOrNull()?.text ?: text
                } else {
                    text // 번역 실패 시 원본 텍스트 반환
                }
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                text // 오류 발생 시 원본 텍스트 반환
            }
        }
    }

    private suspend fun translateDescriptionWithPlaceholder(
        description: String,
        plantName: String,
        targetLang: String
    ): String {
        // translatePlantName에서 학명을 추출
        val pattern = Pattern.compile("^(.*)\\((.*)\\)$")
        val matcher = pattern.matcher(plantName)

        val scientificName = if (matcher.find()) matcher.group(2).trim() else null

        // 학명이 있으면 설명에서 학명을 'A'로 치환
        val tempDescription = if (scientificName != null) {
            description.replace(scientificName, "A")
        } else {
            description
        }

        // 번역 수행
        val translatedDescription = translateText(tempDescription, targetLang)

        // 번역된 설명에서 'A'를 원래 학명으로 복원
        return if (scientificName != null) {
            translatedDescription.replace("A", scientificName)
        } else {
            translatedDescription
        }
    }

    private suspend fun identifyPlantWithApi(photoFile: File) {
        Log.d("Debug2", "identifyPlantWithApi called with file: ${photoFile.absolutePath}")

        withContext(Dispatchers.Main) {
            // 로딩 상태로 오버레이 표시
            showScanOverlay(ModalState.LOADING)
        }

        if (!isNetworkAvailable()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CameraActivity, "네트워크 연결을 확인해주세요", Toast.LENGTH_SHORT).show()
                updateScanOverlayState(ModalState.FAILURE)
            }
            return
        }

        try {
            // 1. 이미지 방향 보정
            val correctedFile = fixImageOrientation(photoFile)
            Log.d("Debug2", "Image orientation fixed: ${correctedFile.absolutePath}")

            // 2. 이미지 최적화
            val optimizedFile = optimizeImage(correctedFile)
            Log.d("Debug2", "Image optimized: ${optimizedFile.absolutePath}")

            // 3. 요청 파일 생성
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), optimizedFile)
            val imagePart =
                MultipartBody.Part.createFormData("images", optimizedFile.name, requestFile)
            Log.d("Debug2", "MultipartBody created for image: ${optimizedFile.name}")

            // 4. API 호출
            Log.d("Debug2", "Making API call to identify plant")
            val response = RetrofitClient.plantApiService.identifyPlant(
                apiKey = BuildConfig.PLANT_API_KEY,
                image = imagePart
            )
            Log.d("Debug2", "API call completed")

            withContext(Dispatchers.Main) {
                if (response.isSuccessful && response.body() != null) {
                    val plantResponse = response.body()!!
                    val suggestion = plantResponse.result.classification.suggestions.firstOrNull()

                    if (suggestion != null) {
                        Log.d("Debug2", "Plant suggestion found: ${suggestion.name}")
                        updateScanOverlayState(ModalState.SUCCESS)

                        val details = suggestion.details
                        val commonNames = details?.common_names?.joinToString(", ") ?: ""
                        val displayName = if (commonNames.isNotEmpty()) {
                            "$commonNames (${suggestion.name})"
                        } else {
                            suggestion.name
                        }
                        val description = details?.description?.value ?: "이 식물에 대한 설명을 찾을 수 없습니다."
                        Log.d("Debug2", "Description: $description")

                        lifecycleScope.launch {
                            val translatedTitle = translatePlantName(displayName, "KO")
                            Log.d("Debug2", "Translated title: $translatedTitle")

                            val translatedDescription =
                                translateDescriptionWithPlaceholder(description, displayName, "KO")
                            Log.d("Debug2", "Translated description: $translatedDescription")

                            // 식물 정보 모달을 표시하기 직전에 성공 모달을 닫음
                            showPlantInfoOverlay(
                                title = translatedTitle,
                                description = translatedDescription,
                                photoFile = optimizedFile
                            )

                            hideScanOverlay() // 성공 모달 닫기

                            // 결과 반환
                            val result = Intent().apply {
                                putExtra("plant_name", translatedTitle)
                                putExtra("plant_description", translatedDescription)
                                putExtra("image_path", optimizedFile.absolutePath)
                                putExtra("captured_date", System.currentTimeMillis())
                            }
                            setResult(RESULT_OK, result)
                        }
                    } else {
                        Log.d("Debug2", "No plant suggestion found in response")
                        updateScanOverlayState(ModalState.FAILURE)
                    }
                } else {
                    Log.d("Debug2", "API response failed: ${response.errorBody()?.string()}")
                    updateScanOverlayState(ModalState.FAILURE)
                }
            }
        } catch (e: Exception) {
            Log.e("Debug2", "Exception occurred during plant identification", e)
            withContext(Dispatchers.Main) {
                updateScanOverlayState(ModalState.FAILURE)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    // 카메라 시작 메서드
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // CameraControl 및 CameraInfo 초기화
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                isCameraActive = true // 카메라 활성화 상태 업데이트


            } catch (exc: Exception) {
                Log.e("CameraActivity", "카메라 바인딩 실패", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            Log.d("Debug2", "Media directory: ${it.absolutePath}")
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        val outputDir = if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
        Log.d("Debug2", "Output directory: ${outputDir.absolutePath}")
        return outputDir
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

    private fun fixImageOrientation(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return file
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val matrix = Matrix().apply { postRotate(rotationAngle) }
            val correctedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // 보정된 이미지 저장
            val correctedFile = File(file.parent, "corrected_${file.name}")
            correctedFile.outputStream().use { out ->
                correctedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            correctedFile
        } catch (e: Exception) {
            Log.e("FixImageOrientation", "Failed to fix image orientation", e)
            file
        }
    }

    private fun showPlantInfoOverlay(title: String, description: String, photoFile: File) {

        // 터치 차단 활성화 (카메라 화면 터치 차단)
        binding.touchBlocker.visibility = View.VISIBLE

        // 카메라 멈추기
        pauseCamera()

        // 제목 및 설명 길이 제한
        val maxTotalLength = 400
        val normalizedDescription = description.replace("\n", " ")
        val titleLength = title.length
        val remainingLength = maxTotalLength - titleLength
        val truncatedDescription = if (normalizedDescription.length > remainingLength) {
            normalizedDescription.substring(0, remainingLength - 5) + "...더보기"
        } else {
            normalizedDescription
        }

        // UI 설정
        binding.plantTitle.text = title
        binding.plantDescription.text = truncatedDescription

        // 이미지 로드
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        if (bitmap != null) {
            binding.plantImage.setImageBitmap(bitmap)
        } else {
            binding.plantImage.setImageResource(R.drawable.modal_info_plant) // 기본 이미지
        }

        // 오버레이 표시
        binding.plantInfoOverlay.visibility = View.VISIBLE

        // 등록 버튼 클릭 리스너
        binding.registerButton.setOnClickListener {
            binding.registerButton.isEnabled = false // 중복 클릭 방지
            lifecycleScope.launch {
                try {
                    registerPlant(title, description, photoFile)
                    hideScanOverlay() // 모달을 먼저 숨김
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToHome() // 모달이 닫힌 후 화면 전환
                    }, 300) // 모달 숨김 애니메이션 시간 이후 실행
                } catch (e: Exception) {
                    Log.e("Register", "Plant registration failed", e)
                    Toast.makeText(
                        this@CameraActivity,
                        "도감 등록 실패: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.registerButton.isEnabled = true // 실패 시 버튼 활성화
                }
            }
        }

        // 닫기 버튼 클릭 리스너
        binding.closeButton.setOnClickListener {
            binding.touchBlocker.visibility = View.GONE // 터치 차단 해제
            // 카메라 액티비티에 남아 있도록 오버레이만 숨김
            binding.plantInfoOverlay.visibility = View.GONE
            resumeCamera()
        }
    }

    private fun pauseCamera() {
        if (::cameraProvider.isInitialized && isCameraActive) {
            cameraProvider.unbindAll()
            isCameraActive = false
            Log.d("Debug2", "Camera paused")
        }
    }

    private fun resumeCamera() {
        if (::cameraProvider.isInitialized && !isCameraActive) {
            startCamera()
            isCameraActive = true
            Log.d("Debug2", "Camera resumed")
        }
    }

    private fun navigateToHome() {
        finish() // CameraActivity 종료
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // 전환 애니메이션 설정
    }

    private suspend fun registerPlant(title: String, description: String, photoFile: File) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        // Step 1: Firebase Storage에 이미지 업로드
        val uploadedImageUrl = uploadImageToFirebaseStorage(photoFile)
        if (uploadedImageUrl != null) {
            // Step 2: Firestore에 데이터 저장
            saveToFirestore(currentUser.uid, uploadedImageUrl, title, description)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CameraActivity, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadImageToFirebaseStorage(photoFile: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val imageRef = storageRef.child("images/${photoFile.name}")

                imageRef.putFile(Uri.fromFile(photoFile)).await()
                Log.d("FirebaseStorage", "이미지 업로드 성공: ${photoFile.name}")

                val downloadUrl = imageRef.downloadUrl.await().toString()
                Log.d("FirebaseStorage", "다운로드 URL: $downloadUrl")
                downloadUrl
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w("FirebaseStorage", "이미지 업로드 작업이 취소되었습니다.", e)
                } else {
                    Log.e("FirebaseStorage", "이미지 업로드 실패", e)
                }
                null
            }
        }
    }

    private suspend fun saveToFirestore(
        userId: String,
        imagePath: String,
        title: String,
        description: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val plant = Plant(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = title,
                    description = description,
                    imagePath = imagePath,
                    captureDate = System.currentTimeMillis(),
                    registrationDate = System.currentTimeMillis()
                )

                val userRef = firestore.collection("users").document(userId)
                val plantRef = firestore.collection("plants").document(plant.id)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val currentExperience = snapshot.getLong("experience") ?: 0
                    val currentCoins = snapshot.getLong("coins") ?: 0

                    val updatedExperience = currentExperience + 15
                    val updatedCoins = currentCoins + 10

                    transaction.set(plantRef, plant)
                    transaction.update(userRef, "experience", updatedExperience)
                    transaction.update(userRef, "coins", updatedCoins)
                }.await()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w("Firestore", "Firestore 작업이 취소되었습니다.", e)
                } else {
                    Log.e("Firestore", "Firestore 저장 실패", e)
                }
                throw e // 상위 호출에서 처리
            }
        }
    }

        private fun handleError(message: String) {
            Toast.makeText(this, "$message. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        }

        private fun showScanOverlay(state: ModalState) {
            // 터치 차단 활성화 (카메라 화면 터치 차단)
            binding.touchBlocker.visibility = View.VISIBLE


            // 오버레이 표시
            binding.modalOverlay.visibility = View.VISIBLE
            updateScanOverlayState(state)
            if (state == ModalState.LOADING) {
                pauseCamera() // 로딩 상태에서 카메라 정지
            }
        }

        private fun hideScanOverlay() {
            // 모달 상태 초기화
            resetModalState()
            // 모달 숨김
            binding.modalOverlay.visibility = View.GONE

            // 터치 차단 해제
            binding.touchBlocker.visibility = View.GONE
        }

        private fun updateScanOverlayState(state: ModalState) {
            when (state) {
                ModalState.LOADING -> {
                    // 로딩 상태 - 아이콘 회전 애니메이션
                    binding.modalBg.setImageResource(R.drawable.modal_bg_loading)
                    binding.modalIcon.setImageResource(R.drawable.ic_modal_loading)
                    binding.modalTitle.text = "로딩 중입니다..."
                    binding.modalMessage.text = "잠시만 기다려주세요!"

                    val rotationAnimator =
                        ObjectAnimator.ofFloat(binding.modalIcon, "rotation", 0f, 360f).apply {
                            duration = 2000
                            repeatCount = ObjectAnimator.INFINITE
                            repeatMode = ObjectAnimator.RESTART
                        }
                    rotationAnimator.start()
                    binding.modalIcon.tag = rotationAnimator // 애니메이션 참조 저장
                }

                ModalState.SUCCESS -> {
                    transitionFromLoadingToResult(
                        resultBg = R.drawable.modal_bg_success,
                        resultIcon = R.drawable.ic_modal_success,
                        title = "식물을 찾았어요!",
                        message = "이제 이 친구에 대해 알려드릴게요!"
                    )
                }

                ModalState.FAILURE -> {
                    transitionFromLoadingToResult(
                        resultBg = R.drawable.modal_bg_failure,
                        resultIcon = R.drawable.ic_modal_failure,
                        title = "식물을 찾지 못했어요...",
                        message = "다시 한 번 촬영해볼까요?"
                    )

                    // 실패 상태도 일정 시간 후 자동 닫기
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideScanOverlay()
                        resumeCamera() // 실패 후 카메라 재시작
                    }, 2500) // 2.5초 유지
                }
            }
        }

        private fun transitionFromLoadingToResult(
            resultBg: Int,
            resultIcon: Int,
            title: String,
            message: String
        ) {
            // 1. 로딩 아이콘 숨기기
            binding.modalIcon.visibility = View.GONE
            binding.modalIconResult.apply {
                visibility = View.VISIBLE
                setImageResource(resultIcon)
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
            }

            // 2. 텍스트 설정
            binding.modalTitle.text = title
            binding.modalMessage.text = message
            binding.modalTitle.alpha = 0f
            binding.modalMessage.alpha = 0f

            // 3. 배경 업데이트
            binding.modalBg.setImageResource(resultBg)

            // 4. 결과 애니메이션
            val iconExpandAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.modalIconResult, "scaleX", 0f, 1f),
                    ObjectAnimator.ofFloat(binding.modalIconResult, "scaleY", 0f, 1f),
                    ObjectAnimator.ofFloat(binding.modalIconResult, "alpha", 0f, 1f)
                )
                duration = 500
            }

            val fadeInTextAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(binding.modalTitle, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(binding.modalMessage, "alpha", 0f, 1f)
                )
                duration = 500
            }

            AnimatorSet().apply {
                playSequentially(iconExpandAnimator, fadeInTextAnimator)
                start()
            }.doOnEnd {
                closeOverlayAfterDelay() // 결과 애니메이션 종료 후 자동 닫기
            }
        }

        private fun AnimatorSet.doOnEnd(action: () -> Unit) {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    action()
                    removeListener(this)
                }
            })
        }

        private fun resetModalState() {
            // 결과 아이콘 숨기기 및 초기화
            binding.modalIconResult.apply {
                visibility = View.GONE
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
            }

            // 로딩 아이콘 다시 활성화
            binding.modalIcon.apply {
                visibility = View.VISIBLE
                rotation = 0f // 로딩 아이콘 회전 초기화
            }

            // 텍스트 초기화
            binding.modalTitle.text = ""
            binding.modalMessage.text = ""
        }


        private fun closeOverlayAfterDelay() {
            Handler(Looper.getMainLooper()).postDelayed({
                resetModalState() // 모달 상태 초기화
                binding.modalOverlay.visibility = View.GONE // 모달 숨기기
            }, 2500) // 2.5초 후 실행
        }
    }
