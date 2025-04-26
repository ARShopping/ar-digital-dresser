package com.example.fyp

import android.graphics.PointF
import kotlin.math.sqrt

data class BodyMesh(
    val neck: PointF,
    val shoulders: Pair<PointF, PointF>,
    val hips: Pair<PointF, PointF>,
    val torsoCenter: PointF,
    val shoulderWidth: Float,
    val torsoHeight: Float,
    val hipWidth: Float,
    val armLength: Float,
    val bodyType: BodyType
) {
    companion object {
        val EMPTY = BodyMesh(
            neck = PointF(0f, 0f),
            shoulders = Pair(PointF(0f, 0f), PointF(0f, 0f)),
            hips = Pair(PointF(0f, 0f), PointF(0f, 0f)),
            torsoCenter = PointF(0f, 0f),
            shoulderWidth = 0f,
            torsoHeight = 0f,
            hipWidth = 0f,
            armLength = 0f,
            bodyType = BodyType.RECTANGLE
        )
    }
}

enum class BodyType {
    HOURGLASS, TRIANGLE, INVERTED_TRIANGLE, RECTANGLE
}

data class ClothingPosition(
    val center: PointF,
    val width: Float,
    val height: Float,
    val rotation: Float
)
