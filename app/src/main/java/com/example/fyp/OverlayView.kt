package com.example.fyp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.SizeF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // Paint objects for different drawing elements
    private val landmarkPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
        strokeWidth = 8f
    }

    private val selectedLandmarkPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
        strokeWidth = 10f
    }

    private val limbPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val bodyContourPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
        alpha = 180
    }

    private val maskPaint = Paint().apply {
        isAntiAlias = true
        alpha = 128
    }

    private val debugPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 32f
        isAntiAlias = true
    }

    private val clothingPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    // Transformation variables
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f
    private var isScaling = false
    private var isDragging = false

    private val scaleGestureDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
                invalidate()
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isScaling = true
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isScaling = false
            }
        })

    // Visualization control flags
    var showDebugInfo: Boolean = false
    var isBackCamera: Boolean = false
    var showPoseLandmarks: Boolean = true
    var showBodyContour: Boolean = true
    var showLimbs: Boolean = true

    // Image properties
    var segmentationMask: Bitmap? = null
        set(value) {
            field = value
            value?.let {
                scaledSegmentationMask = Bitmap.createScaledBitmap(it, width, height, false)
            }
            invalidate()
        }

    private var scaledSegmentationMask: Bitmap? = null

    var overlayBitmap: Bitmap? = null
        set(value) {
            field = value
            fadeInOverlay()
            invalidate()
        }

    // Pose tracking variables
    private var latestPose: Pose? = null
    private var cameraImageWidth = 480f
    private var cameraImageHeight = 640f
    var mirrorHorizontally: Boolean = false

    private var selectedLandmarkType: Int? = null
    internal val smoothedLandmarks = mutableMapOf<Int, PointF>()
    private val smoothingFactor = 0.3f

    var onLandmarkSelected: ((landmarkType: Int) -> Unit)? = null

    // Animation variables
    private var overlayAlpha: Int = 255
    private var fadeStartTime: Long = 0
    private var isFadingIn = false

    // Body mesh visualization
    private val bodyContourPath = Path()
    private val bodyMeshPoints = mutableListOf<PointF>()

    fun getSmoothedLandmark(landmarkType: Int): PointF? {
        return smoothedLandmarks[landmarkType]
    }

    fun setCameraImageSize(width: Int, height: Int) {
        cameraImageWidth = width.toFloat()
        cameraImageHeight = height.toFloat()
    }

    fun setPose(pose: Pose) {
        if (pose.allPoseLandmarks.isNotEmpty()) {
            latestPose = pose
            updateSmoothedLandmarks(pose)
            updateBodyContour()
            invalidate()
        }
    }

    private fun updateSmoothedLandmarks(pose: Pose) {
        for (landmark in pose.allPoseLandmarks) {
            if (landmark.inFrameLikelihood < 0.4f) continue
            val type = landmark.landmarkType
            val rawPoint = landmark.position
            val previous = smoothedLandmarks[type]

            smoothedLandmarks[type] = if (previous == null) {
                PointF(rawPoint.x, rawPoint.y)
            } else {
                PointF(
                    smoothingFactor * previous.x + (1 - smoothingFactor) * rawPoint.x,
                    smoothingFactor * previous.y + (1 - smoothingFactor) * rawPoint.y
                )
            }
        }
    }

    private fun updateBodyContour() {
        bodyContourPath.reset()
        bodyMeshPoints.clear()

        // Create body contour using key landmarks
        val leftShoulder = smoothedLandmarks[PoseLandmark.LEFT_SHOULDER]
        val rightShoulder = smoothedLandmarks[PoseLandmark.RIGHT_SHOULDER]
        val leftHip = smoothedLandmarks[PoseLandmark.LEFT_HIP]
        val rightHip = smoothedLandmarks[PoseLandmark.RIGHT_HIP]
        val leftElbow = smoothedLandmarks[PoseLandmark.LEFT_ELBOW]
        val rightElbow = smoothedLandmarks[PoseLandmark.RIGHT_ELBOW]
        val leftWrist = smoothedLandmarks[PoseLandmark.LEFT_WRIST]
        val rightWrist = smoothedLandmarks[PoseLandmark.RIGHT_WRIST]
        val leftKnee = smoothedLandmarks[PoseLandmark.LEFT_KNEE]
        val rightKnee = smoothedLandmarks[PoseLandmark.RIGHT_KNEE]
        val leftAnkle = smoothedLandmarks[PoseLandmark.LEFT_ANKLE]
        val rightAnkle = smoothedLandmarks[PoseLandmark.RIGHT_ANKLE]

        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            // Start from left shoulder
            bodyContourPath.moveTo(leftShoulder.x, leftShoulder.y)
            bodyMeshPoints.add(leftShoulder)

            // Add left arm if available
            if (leftElbow != null && leftWrist != null) {
                bodyContourPath.lineTo(leftElbow.x, leftElbow.y)
                bodyContourPath.lineTo(leftWrist.x, leftWrist.y)
                bodyMeshPoints.add(leftElbow)
                bodyMeshPoints.add(leftWrist)
            }

            // Add left side of torso
            bodyContourPath.lineTo(leftHip.x, leftHip.y)
            bodyMeshPoints.add(leftHip)

            // Add left leg if available
            if (leftKnee != null && leftAnkle != null) {
                bodyContourPath.lineTo(leftKnee.x, leftKnee.y)
                bodyContourPath.lineTo(leftAnkle.x, leftAnkle.y)
                bodyMeshPoints.add(leftKnee)
                bodyMeshPoints.add(leftAnkle)
            }

            // Add right leg if available
            if (rightKnee != null && rightAnkle != null) {
                bodyContourPath.lineTo(rightAnkle.x, rightAnkle.y)
                bodyContourPath.lineTo(rightKnee.x, rightKnee.y)
                bodyMeshPoints.add(rightAnkle)
                bodyMeshPoints.add(rightKnee)
            }

            // Add right side of torso
            bodyContourPath.lineTo(rightHip.x, rightHip.y)
            bodyMeshPoints.add(rightHip)

            // Add right arm if available
            if (rightElbow != null && rightWrist != null) {
                bodyContourPath.lineTo(rightElbow.x, rightElbow.y)
                bodyContourPath.lineTo(rightWrist.x, rightWrist.y)
                bodyMeshPoints.add(rightElbow)
                bodyMeshPoints.add(rightWrist)
            }

            // Complete the contour
            bodyContourPath.lineTo(rightShoulder.x, rightShoulder.y)
            bodyContourPath.close()
            bodyMeshPoints.add(rightShoulder)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val scaleX = width / cameraImageWidth
        val scaleY = height / cameraImageHeight

        // Draw segmentation mask if available
        scaledSegmentationMask?.let {
            canvas.drawBitmap(it, 0f, 0f, maskPaint)
        }

        // Draw body contour
        if (showBodyContour && !bodyContourPath.isEmpty) {
            val saved = canvas.save()
            canvas.scale(scaleX, scaleY)
            canvas.drawPath(bodyContourPath, bodyContourPaint)
            canvas.restoreToCount(saved)
        }

        // Draw limbs
        if (showLimbs) {
            for ((startType, endType) in poseConnections) {
                val start = smoothedLandmarks[startType]
                val end = smoothedLandmarks[endType]
                if (start == null || end == null) continue

                var startX = start.x * scaleX
                var endX = end.x * scaleX
                val startY = start.y * scaleY
                val endY = end.y * scaleY

                if (mirrorHorizontally) {
                    startX = width - startX
                    endX = width - endX
                }

                canvas.drawLine(startX, startY, endX, endY, limbPaint)
            }
        }

        // Draw pose landmarks
        if (showPoseLandmarks) {
            for ((type, raw) in smoothedLandmarks) {
                val position = scaleAndMirrorPoint(raw, scaleX, scaleY)
                val paint = if (type == selectedLandmarkType) selectedLandmarkPaint else landmarkPaint
                canvas.drawCircle(position.x, position.y, 12f, paint)

                // Draw landmark type for debugging
                if (showDebugInfo) {
                    canvas.drawText(type.toString(), position.x + 15f, position.y, debugPaint)
                }
            }
        }

        // Draw overlay bitmap with perspective transformation
        overlayBitmap?.let { bitmap ->
            val leftShoulder = smoothedLandmarks[PoseLandmark.LEFT_SHOULDER]
            val rightShoulder = smoothedLandmarks[PoseLandmark.RIGHT_SHOULDER]
            val leftHip = smoothedLandmarks[PoseLandmark.LEFT_HIP]
            val rightHip = smoothedLandmarks[PoseLandmark.RIGHT_HIP]

            if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
                val matrix = createClothingTransformMatrix(bitmap,
                    leftShoulder, rightShoulder, leftHip, rightHip, scaleX, scaleY)

                // Apply additional transformations
                matrix.postScale(scaleFactor, scaleFactor,
                    (leftShoulder.x + rightShoulder.x) * scaleX / 2f,
                    (leftShoulder.y + leftHip.y) * scaleY / 2f)

                matrix.postTranslate(posX, posY)

                // Save canvas state
                val saveCount = canvas.save()

                // Clip to body contour if available
                if (!bodyContourPath.isEmpty) {
                    val clipPath = Path(bodyContourPath)
                    val scaleMatrix = Matrix()
                    scaleMatrix.setScale(scaleX, scaleY)
                    clipPath.transform(scaleMatrix)
                    if (mirrorHorizontally) {
                        val mirrorMatrix = Matrix()
                        mirrorMatrix.setScale(-1f, 1f)
                        mirrorMatrix.postTranslate(width.toFloat(), 0f)
                        clipPath.transform(mirrorMatrix)
                    }
                    canvas.clipPath(clipPath)
                }

                // Draw the clothing
                clothingPaint.alpha = overlayAlpha
                canvas.drawBitmap(bitmap, matrix, clothingPaint)

                // Restore canvas
                canvas.restoreToCount(saveCount)
            }
        }

        // Draw debug information
        if (showDebugInfo) {
            canvas.drawText("Scale: ${"%.2f".format(scaleFactor)}", 20f, height - 120f, debugPaint)
            canvas.drawText("Position: (${"%.1f".format(posX)}, ${"%.1f".format(posY)})", 20f, height - 80f, debugPaint)
            canvas.drawText("Landmarks: ${smoothedLandmarks.size}", 20f, height - 40f, debugPaint)
        }
    }

    private fun createClothingTransformMatrix(
        bitmap: Bitmap,
        leftShoulder: PointF,
        rightShoulder: PointF,
        leftHip: PointF,
        rightHip: PointF,
        scaleX: Float,
        scaleY: Float
    ): Matrix {
        val matrix = Matrix()

        // Source points (corners of clothing image)
        val src = floatArrayOf(
            0f, 0f,                        // top-left
            bitmap.width.toFloat(), 0f,    // top-right
            0f, bitmap.height.toFloat(),   // bottom-left
            bitmap.width.toFloat(), bitmap.height.toFloat() // bottom-right
        )

        // Destination points (mapped to body)
        var lsX = leftShoulder.x * scaleX
        var rsX = rightShoulder.x * scaleX
        val lsY = leftShoulder.y * scaleY
        val rsY = rightShoulder.y * scaleY
        var lhX = leftHip.x * scaleX
        var rhX = rightHip.x * scaleX
        val lhY = leftHip.y * scaleY
        val rhY = rightHip.y * scaleY

        if (mirrorHorizontally) {
            lsX = width - lsX
            rsX = width - rsX
            lhX = width - lhX
            rhX = width - rhX
        }

        val dst = floatArrayOf(
            lsX, lsY,  // left shoulder
            rsX, rsY,  // right shoulder
            lhX, lhY,  // left hip
            rhX, rhY   // right hip
        )

        // Create perspective transform
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            // Fallback to simpler transformation if perspective fails
            val scaleX = (dst[2] - dst[0]) / bitmap.width
            val scaleY = (dst[5] - dst[1]) / bitmap.height
            matrix.postScale(scaleX, scaleY)
            matrix.postTranslate(dst[0], dst[1])
        }

        return matrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isScaling) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true

                    // Check for landmark selection
                    if (showPoseLandmarks) {
                        val touchX = event.x
                        val touchY = event.y
                        val scaleX = width / cameraImageWidth
                        val scaleY = height / cameraImageHeight

                        var closestType: Int? = null
                        var minDist = Float.MAX_VALUE

                        for ((type, raw) in smoothedLandmarks) {
                            val pos = scaleAndMirrorPoint(raw, scaleX, scaleY)
                            val dist = hypot(pos.x - touchX, pos.y - touchY)
                            if (dist < 40 && dist < minDist) {
                                minDist = dist
                                closestType = type
                            }
                        }

                        if (closestType != null) {
                            selectedLandmarkType = closestType
                            onLandmarkSelected?.invoke(closestType)
                            invalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && isDragging && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    posX += dx
                    posY += dy

                    // Limit movement range
                    posX = posX.coerceIn(-width * 0.3f, width * 0.3f)
                    posY = posY.coerceIn(-height * 0.3f, height * 0.3f)

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }

        return true
    }

    fun resetTransformations() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        invalidate()
    }

    private fun fadeInOverlay(durationMillis: Long = 300) {
        overlayAlpha = 0
        fadeStartTime = System.currentTimeMillis()
        isFadingIn = true

        object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - fadeStartTime
                val progress = elapsed.toFloat() / durationMillis

                if (progress < 1f) {
                    overlayAlpha = (255 * progress).toInt()
                    invalidate()
                    postDelayed(this, 16) // ~60fps
                } else {
                    overlayAlpha = 255
                    isFadingIn = false
                    invalidate()
                }
            }
        }.run()
    }

    private fun scaleAndMirrorPoint(position: PointF, scaleX: Float, scaleY: Float): PointF {
        var x = position.x * scaleX
        val y = position.y * scaleY
        if (mirrorHorizontally) x = width - x
        return PointF(x, y)
    }

    companion object {
        private val poseConnections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_EAR,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_EAR,
            PoseLandmark.LEFT_EYE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EYE to PoseLandmark.LEFT_EAR,
            PoseLandmark.RIGHT_EYE to PoseLandmark.RIGHT_EAR
        )
    }
}