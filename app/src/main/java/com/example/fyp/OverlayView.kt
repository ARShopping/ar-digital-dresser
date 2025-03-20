package com.example.fyp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.Pose

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val landmarkPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val maskPaint = Paint().apply {
        isAntiAlias = true
        alpha = 128  // Semi-transparent mask
    }

    private var segmentationMask: Bitmap? = null
    private val landmarkPoints = mutableListOf<Pair<Float, Float>>()

    // Toggle to show or hide pose landmarks
    var showPoseLandmarks: Boolean = false

    // Dimensions of the original camera input (used for scaling)
    private var cameraImageWidth = 480f
    private var cameraImageHeight = 640f

    // Whether to mirror horizontally (for front camera)
    var mirrorHorizontally: Boolean = false

    fun setCameraImageSize(width: Int, height: Int) {
        cameraImageWidth = width.toFloat()
        cameraImageHeight = height.toFloat()
    }

    fun setPose(pose: Pose) {
        landmarkPoints.clear()

        val scaleX = width / cameraImageWidth
        val scaleY = height / cameraImageHeight

        for (landmark in pose.allPoseLandmarks) {
            var x = landmark.position.x * scaleX
            val y = landmark.position.y * scaleY

            if (mirrorHorizontally) {
                x = width - x
            }

            landmarkPoints.add(Pair(x, y))
        }

        invalidate()
    }

    fun setSegmentationMask(maskBitmap: Bitmap?) {
        segmentationMask = maskBitmap
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        segmentationMask?.let {
            val scaled = Bitmap.createScaledBitmap(it, width, height, false)
            canvas.drawBitmap(scaled, 0f, 0f, maskPaint)
        }

        if (showPoseLandmarks) {
            for ((x, y) in landmarkPoints) {
                canvas.drawCircle(x, y, 10f, landmarkPaint)
            }
        }
    }
}
