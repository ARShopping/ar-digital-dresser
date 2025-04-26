
package com.example.fyp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.*
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.nio.ByteBuffer
import android.graphics.PointF
import androidx.camera.video.*
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.segmentation.SegmentationMask
import java.nio.BufferUnderflowException
import kotlin.math.*
import java.util.concurrent.RejectedExecutionException

class ARTryOnActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ARTryOnActivity"
        private const val SMOOTHING_WINDOW = 5
        private const val REFERENCE_TORSO_HEIGHT = 160f
    }

    // UI Components
    private lateinit var btnResetTransform: Button
    private lateinit var previewView: PreviewView
    private lateinit var clothingOverlay: ImageView
    private lateinit var btnCaptureImage: Button
    private lateinit var btnRecordVideo: Button
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var recordingIndicator: ImageView
    private lateinit var overlayView: OverlayView

    // Camera and ML Components
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var poseDetector: PoseDetector
    private lateinit var selfieSegmenter: com.google.mlkit.vision.segmentation.Segmenter
    private var imageAnalysis: ImageAnalysis? = null

    // State variables
    private var originalClothingBitmap: Bitmap? = null
    private var maskedClothingBitmap: Bitmap? = null
    private var currentSegmentationMask: Bitmap? = null
    private var recording: Recording? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var isActive = true

    // Body mesh and smoothing
    private val smoothingFilter = SmoothingFilter()
    private val recordingLock = Any()

    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "CameraExecutor").apply {
                priority = Thread.NORM_PRIORITY - 1
            }
        }
    }

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                switchCamera()
                return true
            }
        })
    }

    private val blinkAnimation by lazy {
        AlphaAnimation(1f, 0f).apply {
            duration = 500
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = AlphaAnimation.INFINITE
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (cameraGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }

        if (!audioGranted) {
            Toast.makeText(this, "Audio will not be recorded", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artry_on)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.arFrameLayout)) { view, insets ->
            view.setPadding(0, insets.displayCutout?.safeInsetTop ?: 0, 0, 0)
            insets
        }

        // Initialize views
        previewView = findViewById(R.id.previewView)
        clothingOverlay = findViewById<ImageView>(R.id.clothingOverlay).apply { visibility = View.GONE }
        btnCaptureImage = findViewById(R.id.btnCaptureImage)
        btnRecordVideo = findViewById(R.id.btnRecordVideo)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        recordingIndicator = findViewById(R.id.recordingIndicator)
        overlayView = findViewById(R.id.overlayView)
        btnResetTransform = findViewById(R.id.btnResetTransform)

        btnResetTransform.setOnClickListener {
            overlayView.resetTransformations()
        }

        // Initialize ML Kit components
        poseDetector = PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )

        selfieSegmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .build()
        )

        // Load clothing image
        val clothingUrl = intent.getStringExtra("modelUrl")
        if (!clothingUrl.isNullOrEmpty()) {
            loadAndMaskClothing(clothingUrl)
        } else {
            Toast.makeText(this, "No clothing model provided", Toast.LENGTH_SHORT).show()
        }

        // Set up click listeners
        btnCaptureImage.setOnClickListener { takePhoto() }
        btnRecordVideo.setOnClickListener { toggleRecording() }
        btnSwitchCamera.setOnClickListener { switchCamera() }

        // Lifecycle management
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                isActive = false
                stopRecordingSafely()
            }

            override fun onResume(owner: LifecycleOwner) {
                isActive = true
            }

            override fun onDestroy(owner: LifecycleOwner) {
                isActive = false
                cleanupResources()
            }
        })

        setupTouchGesture()
        requestPermissions()
    }

    private fun loadAndMaskClothing(modelUrl: String) {
        Glide.with(this)
            .asBitmap()
            .load(modelUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    cameraExecutor.execute {
                        originalClothingBitmap = resource
                        overlayView.overlayBitmap = resource

                        currentSegmentationMask?.let { mask ->
                            val scaledMask = Bitmap.createScaledBitmap(mask, resource.width, resource.height, true)
                            maskedClothingBitmap = applyAlphaMask(resource, scaledMask)
                        }

                        safeRunOnUiThread {
                            clothingOverlay.setImageBitmap(maskedClothingBitmap ?: resource)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    safeRunOnUiThread {
                        clothingOverlay.setImageDrawable(null)
                    }
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchGesture() {
        previewView.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) view.performClick()
            true
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        requestPermissionLauncher.launch(permissions)
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        animatePreviewTransition()
        startCamera()
    }

    private fun animatePreviewTransition() {
        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 300; fillAfter = true }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300; fillAfter = true }
        previewView.startAnimation(fadeOut)
        previewView.postDelayed({ previewView.startAnimation(fadeIn) }, 300)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder().build()

                val recorder = Recorder.Builder()
                    .setExecutor(cameraExecutor)
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (isActive) processImageProxy(imageProxy)
                            else imageProxy.close()
                        }
                    }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture, imageAnalysis!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
                safeRunOnUiThread {
                    Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!isActive || isFinishing || isDestroyed) {
            imageProxy.close()
            return
        }

        try {
            val mediaImage = imageProxy.image ?: return imageProxy.close()
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            var poseDone = false
            var segmentDone = false

            poseDetector.process(inputImage)
                .addOnSuccessListener { pose ->
                    if (isActive && !isFinishing) {
                        safeRunOnUiThread {
                            try {
                                if (pose.allPoseLandmarks.isEmpty()) {
                                    clothingOverlay.visibility = View.GONE
                                } else {
                                    overlayView.setPose(pose)
                                    processPoseData(pose)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Pose processing UI error", e)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Pose detection failed", e)
                }
                .addOnCompleteListener {
                    poseDone = true
                    if (segmentDone) imageProxy.close()
                }

            selfieSegmenter.process(inputImage)
                .addOnSuccessListener { mask ->
                    if (isActive) {
                        try {
                            cameraExecutor.execute {
                                try {
                                    currentSegmentationMask = mask.toAlphaMaskBitmap(inputImage.width, inputImage.height)
                                    originalClothingBitmap?.let { clothingBitmap ->
                                        val scaledMask = Bitmap.createScaledBitmap(
                                            currentSegmentationMask!!,
                                            clothingBitmap.width,
                                            clothingBitmap.height,
                                            true
                                        )
                                        maskedClothingBitmap = applyAlphaMask(clothingBitmap, scaledMask)
                                        safeRunOnUiThread {
                                            clothingOverlay.setImageBitmap(maskedClothingBitmap)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Segmentation processing error", e)
                                }
                            }
                        } catch (e: RejectedExecutionException) {
                            Log.e(TAG, "Executor rejected segmentation task", e)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Segmentation failed", e)
                }
                .addOnCompleteListener {
                    segmentDone = true
                    if (poseDone) imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Image processing error", e)
            imageProxy.close()
        }
    }

    private fun processPoseData(pose: Pose) {
        val bodyMesh = calculateBodyMesh(pose)
        if (bodyMesh == BodyMesh.EMPTY) {
            safeRunOnUiThread {
                clothingOverlay.visibility = View.GONE
            }
            return
        }

        val rawPosition = calculateClothingPosition(bodyMesh)
        val smoothedPosition = smoothingFilter.smooth(rawPosition)

        safeRunOnUiThread {
            if (smoothedPosition.width > 50 && smoothedPosition.height > 50) {
                clothingOverlay.visibility = View.VISIBLE

                // Apply transformation matrix
                val matrix = createClothingTransformMatrix(
                    originalClothingBitmap ?: return@safeRunOnUiThread,
                    bodyMesh
                )

                // Apply additional smoothing adjustments
                matrix.postScale(
                    smoothedPosition.width / bodyMesh.shoulderWidth,
                    smoothedPosition.height / bodyMesh.torsoHeight,
                    smoothedPosition.center.x,
                    smoothedPosition.center.y
                )

                matrix.postRotate(
                    smoothedPosition.rotation,
                    smoothedPosition.center.x,
                    smoothedPosition.center.y
                )

                // Apply to ImageView
                clothingOverlay.imageMatrix = matrix
            } else {
                clothingOverlay.visibility = View.GONE
            }
        }
    }

    private fun calculateBodyMesh(pose: Pose): BodyMesh {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.position
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.position

        if (leftShoulder == null || rightShoulder == null || leftHip == null || rightHip == null) {
            return BodyMesh.EMPTY
        }

        // Calculate key points
        val neck = midpoint(leftShoulder, rightShoulder)
        val hips = midpoint(leftHip, rightHip)
        val torsoCenter = midpoint(neck, hips)

        // Calculate dimensions
        val shoulderWidth = distanceBetween(leftShoulder, rightShoulder)
        val torsoHeight = distanceBetween(neck, hips)
        val hipWidth = distanceBetween(leftHip, rightHip)

        // Calculate arm dimensions if available
        val leftArmLength = if (leftElbow != null) {
            distanceBetween(leftShoulder, leftElbow) * 1.8f // Approximate full arm length
        } else {
            torsoHeight * 0.8f // Default estimation
        }

        // Calculate body shape parameters
        val shoulderToHipRatio = shoulderWidth / hipWidth
        val isHourglass = shoulderToHipRatio in 0.9f..1.1f
        val isTriangle = shoulderToHipRatio < 0.9f
        val isInvertedTriangle = shoulderToHipRatio > 1.1f

        return BodyMesh(
            neck = neck,
            shoulders = Pair(leftShoulder, rightShoulder),
            hips = Pair(leftHip, rightHip),
            torsoCenter = torsoCenter,
            shoulderWidth = shoulderWidth,
            torsoHeight = torsoHeight,
            hipWidth = hipWidth,
            armLength = leftArmLength,
            bodyType = when {
                isHourglass -> BodyType.HOURGLASS
                isTriangle -> BodyType.TRIANGLE
                isInvertedTriangle -> BodyType.INVERTED_TRIANGLE
                else -> BodyType.RECTANGLE
            }
        )
    }

    private fun calculateClothingPosition(bodyMesh: BodyMesh): ClothingPosition {
        // Base positioning on torso
        val centerX = bodyMesh.torsoCenter.x
        val centerY = bodyMesh.torsoCenter.y

        // Calculate clothing width based on body type
        val width = when (bodyMesh.bodyType) {
            BodyType.HOURGLASS -> bodyMesh.shoulderWidth * 1.1f
            BodyType.TRIANGLE -> bodyMesh.hipWidth * 1.2f
            BodyType.INVERTED_TRIANGLE -> bodyMesh.shoulderWidth * 1.3f
            BodyType.RECTANGLE -> max(bodyMesh.shoulderWidth, bodyMesh.hipWidth) * 1.15f
        }

        // Calculate height based on torso proportions
        val height = bodyMesh.torsoHeight * 2.2f // Covers from shoulders to hips

        // Calculate rotation based on shoulder angle
        val shoulderAngle = calculateAngle(
            bodyMesh.shoulders.first,
            bodyMesh.neck,
            bodyMesh.shoulders.second
        )

        return ClothingPosition(
            center = PointF(centerX, centerY),
            width = width,
            height = height,
            rotation = shoulderAngle
        )
    }

    private fun createClothingTransformMatrix(
        clothingBitmap: Bitmap,
        bodyMesh: BodyMesh
    ): Matrix {
        val matrix = Matrix()

        // Source points (corners of clothing image)
        val src = floatArrayOf(
            0f, 0f,                        // top-left
            clothingBitmap.width.toFloat(), 0f, // top-right
            0f, clothingBitmap.height.toFloat(), // bottom-left
            clothingBitmap.width.toFloat(), clothingBitmap.height.toFloat() // bottom-right
        )

        // Destination points (mapped to body)
        val dst = floatArrayOf(
            bodyMesh.shoulders.first.x, bodyMesh.shoulders.first.y, // left shoulder
            bodyMesh.shoulders.second.x, bodyMesh.shoulders.second.y, // right shoulder
            bodyMesh.hips.first.x, bodyMesh.hips.first.y, // left hip
            bodyMesh.hips.second.x, bodyMesh.hips.second.y  // right hip
        )

        // Create perspective transform
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            // Fallback to simpler transformation if perspective fails
            val scaleX = (dst[2] - dst[0]) / clothingBitmap.width
            val scaleY = (dst[5] - dst[1]) / clothingBitmap.height
            matrix.postScale(scaleX, scaleY)
            matrix.postTranslate(dst[0], dst[1])
        }

        return matrix
    }

    private fun stopRecordingSafely() {
        synchronized(recordingLock) {
            recording?.let { rec ->
                try {
                    rec.stop()
                    recording = null
                    safeRunOnUiThread {
                        btnRecordVideo.text = "Record"
                        stopBlinkingIndicator()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping recording", e)
                    recording = null
                }
            }
        }
    }

    private fun toggleRecording() {
        synchronized(recordingLock) {
            if (recording != null) {
                stopRecordingSafely()
            } else {
                val file = File(
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
                )
                val outputOptions = FileOutputOptions.Builder(file).build()

                try {
                    val pending = videoCapture.output.prepareRecording(this, outputOptions)
                    recording = if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        pending.withAudioEnabled()
                    } else {
                        pending
                    }.start(ContextCompat.getMainExecutor(this)) {
                        when (it) {
                            is VideoRecordEvent.Start -> {
                                safeRunOnUiThread {
                                    btnRecordVideo.text = "Stop"
                                    startBlinkingIndicator()
                                }
                            }
                            is VideoRecordEvent.Finalize -> {
                                safeRunOnUiThread {
                                    btnRecordVideo.text = "Record"
                                    stopBlinkingIndicator()
                                    if (it.error == null) {
                                        Toast.makeText(this, "Video saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start recording", e)
                    safeRunOnUiThread {
                        Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun takePhoto() {
        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exc)
                    safeRunOnUiThread {
                        Toast.makeText(this@ARTryOnActivity, "Photo error: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    safeRunOnUiThread {
                        Toast.makeText(this@ARTryOnActivity, "Saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun startBlinkingIndicator() {
        safeRunOnUiThread {
            recordingIndicator.visibility = View.VISIBLE
            recordingIndicator.startAnimation(blinkAnimation)
        }
    }

    private fun stopBlinkingIndicator() {
        safeRunOnUiThread {
            recordingIndicator.clearAnimation()
            recordingIndicator.visibility = View.GONE
        }
    }

    private fun safeRunOnUiThread(action: () -> Unit) {
        if (!isFinishing && !isDestroyed && isActive) {
            runOnUiThread {
                if (!isFinishing && !isDestroyed && isActive) {
                    try {
                        action()
                    } catch (e: Exception) {
                        Log.e(TAG, "UI update failed", e)
                    }
                }
            }
        }
    }

    private fun cleanupResources() {
        try {
            cameraExecutor.shutdown()
            if (!cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                cameraExecutor.shutdownNow()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down executor", e)
        }

        imageAnalysis?.clearAnalyzer()

        originalClothingBitmap?.recycle()
        maskedClothingBitmap?.recycle()
        currentSegmentationMask?.recycle()
    }

    private fun SegmentationMask.toAlphaMaskBitmap(width: Int, height: Int): Bitmap {
        val maskBuffer: ByteBuffer = this.buffer
        maskBuffer.rewind()

        // Validate buffer capacity
        val requiredCapacity = width * height * java.lang.Float.BYTES
        if (maskBuffer.remaining() < requiredCapacity) {
            Log.e(TAG, "Insufficient buffer capacity for mask dimensions")
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val floatBuffer = maskBuffer.asFloatBuffer()

        for (y in 0 until height) {
            for (x in 0 until width) {
                try {
                    val confidence = floatBuffer.get()
                    val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                    val color = Color.argb(alpha, 0, 0, 0)
                    bitmap.setPixel(x, y, color)
                } catch (e: BufferUnderflowException) {
                    Log.e(TAG, "Buffer underflow at ($x,$y)", e)
                    bitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }
        return bitmap
    }

    private fun applyAlphaMask(clothingBitmap: Bitmap, alphaMask: Bitmap): Bitmap {
        val result = clothingBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(alphaMask, 0f, 0f, paint)
        return result
    }

    // Helper functions
    private fun distanceBetween(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    private fun midpoint(p1: PointF, p2: PointF): PointF {
        return PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
    }

    private fun calculateAngle(a: PointF, b: PointF, c: PointF): Float {
        val ab = PointF(b.x - a.x, b.y - a.y)
        val cb = PointF(b.x - c.x, b.y - c.y)

        val dot = ab.x * cb.x + ab.y * cb.y
        val cross = ab.x * cb.y - ab.y * cb.x

        return atan2(cross, dot) * (180 / PI).toFloat()
    }

    private class SmoothingFilter(private val windowSize: Int = 5) {
        private val xHistory = ArrayDeque<Float>()
        private val yHistory = ArrayDeque<Float>()
        private val widthHistory = ArrayDeque<Float>()
        private val heightHistory = ArrayDeque<Float>()
        private val rotationHistory = ArrayDeque<Float>()

        fun smooth(position: ClothingPosition): ClothingPosition {
            // Add current values to history
            xHistory.add(position.center.x)
            yHistory.add(position.center.y)
            widthHistory.add(position.width)
            heightHistory.add(position.height)
            rotationHistory.add(position.rotation)

            // Maintain window size
            if (xHistory.size > windowSize) xHistory.removeFirst()
            if (yHistory.size > windowSize) yHistory.removeFirst()
            if (widthHistory.size > windowSize) widthHistory.removeFirst()
            if (heightHistory.size > windowSize) heightHistory.removeFirst()
            if (rotationHistory.size > windowSize) rotationHistory.removeFirst()

            // Apply weighted average (more weight to recent values)
            val weights = List(windowSize) { i -> (i + 1).toFloat() / windowSize }

            fun weightedAverage(values: List<Float>): Float {
                val effectiveWeights = weights.take(values.size)
                val weightedSum = values.zip(effectiveWeights).sumOf { (v, w) -> (v * w).toDouble() }
                val totalWeight = effectiveWeights.sumOf { it.toDouble() }
                return (weightedSum / totalWeight).toFloat()
            }

            return ClothingPosition(
                center = PointF(
                    weightedAverage(xHistory.toList()),
                    weightedAverage(yHistory.toList())
                ),
                width = weightedAverage(widthHistory.toList()),
                height = weightedAverage(heightHistory.toList()),
                rotation = weightedAverage(rotationHistory.toList())
            )
        }
    }
}