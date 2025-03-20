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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.*
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import java.nio.ByteBuffer
import android.graphics.PointF
import androidx.camera.video.*
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


class ARTryOnActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var clothingOverlay: ImageView
    private lateinit var poseDetector: PoseDetector
    private lateinit var selfieSegmenter: com.google.mlkit.vision.segmentation.Segmenter
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var btnCaptureImage: Button
    private lateinit var btnRecordVideo: Button
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var recordingIndicator: ImageView
    private lateinit var overlayView: OverlayView

    private var recording: Recording? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var imageAnalysis: ImageAnalysis? = null

    private val smoothingWindow = 5
    private val widthHistory = ArrayDeque<Float>()
    private val heightHistory = ArrayDeque<Float>()
    private val xHistory = ArrayDeque<Float>()
    private val yHistory = ArrayDeque<Float>()

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

        if (cameraGranted) startCamera()
        else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()

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

        previewView = findViewById(R.id.previewView)
        clothingOverlay = findViewById(R.id.clothingOverlay)
        btnCaptureImage = findViewById(R.id.btnCaptureImage)
        btnRecordVideo = findViewById(R.id.btnRecordVideo)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        recordingIndicator = findViewById(R.id.recordingIndicator)
        overlayView = findViewById(R.id.overlayView)

        val modelUrl = intent.getStringExtra("modelUrl")
        if (!modelUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(modelUrl)
                .into(clothingOverlay)
            clothingOverlay.bringToFront()
        } else {
            Toast.makeText(this, "No clothing model provided", Toast.LENGTH_SHORT).show()
        }

        poseDetector = PoseDetection.getClient(
            PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
        )

        selfieSegmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.STREAM_MODE).build()
        )

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnCaptureImage.setOnClickListener { takePhoto() }
        btnRecordVideo.setOnClickListener { toggleRecording() }
        btnSwitchCamera.setOnClickListener { switchCamera() }

        setupTouchGesture()
        requestPermissions()
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
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processImageProxy(imageProxy) }
                }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture, imageAnalysis!!)
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        var poseDone = false
        var segmentDone = false

        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                runOnUiThread {
                    overlayView.setPose(pose)
                    processPoseData(pose)
                }
            }
            .addOnCompleteListener {
                poseDone = true
                if (segmentDone) imageProxy.close()
            }

        selfieSegmenter.process(inputImage)
            .addOnSuccessListener { mask ->
                cameraExecutor.execute {
                    val maskBitmap = mask.toAlphaMaskBitmap(inputImage.width, inputImage.height)

                    val modelUrl = intent.getStringExtra("modelUrl")
                    if (!modelUrl.isNullOrEmpty()) {
                        runOnUiThread {
                            Glide.with(this)
                                .asBitmap()
                                .load(modelUrl)
                                .into(object : CustomTarget<Bitmap>() {
                                    override fun onResourceReady(clothingBitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                        cameraExecutor.execute {
                                            val scaledMask = Bitmap.createScaledBitmap(maskBitmap, clothingBitmap.width, clothingBitmap.height, true)
                                            val maskedClothing = applyAlphaMask(clothingBitmap, scaledMask)
                                            runOnUiThread {
                                                clothingOverlay.setImageBitmap(maskedClothing)
                                            }
                                        }
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        clothingOverlay.setImageDrawable(null)
                                    }
                                })
                        }
                    }
                }
            }
            .addOnCompleteListener {
                segmentDone = true
                if (poseDone) imageProxy.close()
            }
    }

    fun SegmentationMask.toAlphaMaskBitmap(width: Int, height: Int): Bitmap {
        val maskBuffer: ByteBuffer = this.buffer
        maskBuffer.rewind()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val confidence = maskBuffer.float
                val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                val color = Color.argb(alpha, 0, 0, 0)
                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }

    fun applyAlphaMask(clothingBitmap: Bitmap, alphaMask: Bitmap): Bitmap {
        val width = clothingBitmap.width
        val height = clothingBitmap.height
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val clothingPixel = clothingBitmap.getPixel(x, y)
                val maskPixel = alphaMask.getPixel(x, y)
                val alpha = Color.alpha(maskPixel)
                val newPixel = Color.argb(alpha, Color.red(clothingPixel), Color.green(clothingPixel), Color.blue(clothingPixel))
                resultBitmap.setPixel(x, y, newPixel)
            }
        }

        return resultBitmap
    }

    private fun processPoseData(pose: Pose) {
        val previewWidth = previewView.width.toFloat()

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position

        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            val ls = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                PointF(previewWidth - leftShoulder.x, leftShoulder.y) else leftShoulder

            val rs = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                PointF(previewWidth - rightShoulder.x, rightShoulder.y) else rightShoulder

            val lh = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                PointF(previewWidth - leftHip.x, leftHip.y) else leftHip

            val rh = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                PointF(previewWidth - rightHip.x, rightHip.y) else rightHip

            val shoulderDist = rs.x - ls.x
            val height = (lh.y + rh.y) / 2 - (ls.y + rs.y) / 2

            // ✅ Log values to help with debugging
            Log.d("POSE", "ShoulderDist: $shoulderDist, Height: $height")

            // ✅ Loosen thresholds to allow overlay to show more easily
            if (shoulderDist > 20 && height > 50) {
                val centerX = (ls.x + rs.x) / 2
                val centerY = (ls.y + (lh.y + rh.y) / 2) / 2
                smoothAndApplyOverlay(centerX, centerY, shoulderDist, height)
            } else {
                clothingOverlay.visibility = View.GONE
            }
        } else {
            clothingOverlay.visibility = View.GONE
        }
    }


    private fun smoothAndApplyOverlay(x: Float, y: Float, width: Float, height: Float) {
        xHistory.add(x)
        yHistory.add(y)
        widthHistory.add(width)
        heightHistory.add(height)

        if (xHistory.size > smoothingWindow) {
            xHistory.removeFirst()
            yHistory.removeFirst()
            widthHistory.removeFirst()
            heightHistory.removeFirst()
        }

        val smoothedX = xHistory.average().toFloat()
        val smoothedY = yHistory.average().toFloat()
        val smoothedWidth = widthHistory.average().toFloat()
        val smoothedHeight = heightHistory.average().toFloat()

        clothingOverlay.animate()
            .x(smoothedX - smoothedWidth / 2)
            .y(smoothedY - smoothedHeight / 2)
            .setDuration(100)
            .start()

        val newLayoutParams = clothingOverlay.layoutParams
        newLayoutParams.width = smoothedWidth.roundToInt()
        newLayoutParams.height = smoothedHeight.roundToInt()
        clothingOverlay.layoutParams = newLayoutParams

        clothingOverlay.scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT) -1f else 1f

        // ✅ Temporary debug aid: make overlay visible with a semi-transparent red color
        clothingOverlay.setBackgroundColor(Color.argb(100, 255, 0, 0))

        clothingOverlay.visibility = View.VISIBLE
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
                    Toast.makeText(this@ARTryOnActivity, "Photo error: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@ARTryOnActivity, "Saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun toggleRecording() {
        if (recording != null) {
            recording?.stop()
            recording = null
            btnRecordVideo.text = "Record"
            stopBlinkingIndicator()
        } else {
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
            )
            val outputOptions = FileOutputOptions.Builder(file).build()
            val pending = videoCapture.output.prepareRecording(this, outputOptions)
            recording = if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                pending.withAudioEnabled()
            } else {
                pending
            }.start(ContextCompat.getMainExecutor(this)) {
                when (it) {
                    is VideoRecordEvent.Start -> {
                        btnRecordVideo.text = "Stop"
                        startBlinkingIndicator()
                    }
                    is VideoRecordEvent.Finalize -> {
                        btnRecordVideo.text = "Record"
                        stopBlinkingIndicator()
                        Toast.makeText(this, "Video saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startBlinkingIndicator() {
        recordingIndicator.visibility = View.VISIBLE
        recordingIndicator.startAnimation(blinkAnimation)
    }

    private fun stopBlinkingIndicator() {
        recordingIndicator.clearAnimation()
        recordingIndicator.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (!::cameraExecutor.isInitialized || cameraExecutor.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
    }
}
