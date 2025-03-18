// FaceAnalysisService.kt
package com.example.recognicam.data.sensor

import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class FaceMetrics(
    val lookAwayCount: Int = 0,
    val attentionDurationMs: Long = 0,
    val totalLookAwayTimeMs: Long = 0,
    val blinkCount: Int = 0,
    val blinkRate: Float = 0f,
    val emotionChanges: Int = 0,
    val faceVisiblePercentage: Int = 0
)

class FaceAnalysisService {
    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        FaceDetection.getClient(options)
    }

    // Metrics
    private var startTime = 0L
    private var lookAwayCount = 0
    private var totalFrames = 0
    private var framesWithFace = 0
    private var blinkCount = 0
    private var emotionChanges = 0
    private var lastEmotion = "neutral"
    private var totalLookAwayTimeMs = 0L
    private var lastLookAwayStartTime = 0L
    private var isLookingAway = false

    // Blink detection
    private var lastLeftEyeOpen = true
    private var lastRightEyeOpen = true
    private var potentialBlinkStartTime = 0L
    private var isInBlink = false

    // State flow for real-time updates
    private val _faceMetrics = MutableStateFlow(FaceMetrics())
    val faceMetrics: StateFlow<FaceMetrics> = _faceMetrics.asStateFlow()

    fun start() {
        startTime = System.currentTimeMillis()
        lookAwayCount = 0
        totalFrames = 0
        framesWithFace = 0
        blinkCount = 0
        emotionChanges = 0
        lastEmotion = "neutral"
        totalLookAwayTimeMs = 0L
        lastLookAwayStartTime = 0L
        isLookingAway = false
        lastLeftEyeOpen = true
        lastRightEyeOpen = true
        isInBlink = false
        _faceMetrics.value = FaceMetrics()
    }

    fun reset() {
        start()
    }

    fun processImage(imageProxy: ImageProxy) {
        totalFrames++

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    processFaces(faces)
                    updateMetrics()
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalysis", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            handleNoFace()
            updateMetrics()
        }
    }

    private fun processFaces(faces: List<Face>) {
        if (faces.isEmpty()) {
            handleNoFace()
            return
        }

        framesWithFace++

        // Get most prominent face
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces[0]

        // Check if face is large enough to analyze properly
        val faceWidth = face.boundingBox.width()
        val faceHeight = face.boundingBox.height()
        if (faceWidth < 100 || faceHeight < 100) {
            // Face too small or partial
            handleNoFace()
            return
        }

        // Process head position for attention
        val rotY = face.headEulerAngleY // Head rotation (left/right)
        val rotX = face.headEulerAngleX // Head rotation (up/down)

        // Detect look away
        val headLookingAway = abs(rotY) > 25f || abs(rotX) > 20f

        // Process eye states for blink detection
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: -1f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: -1f

        // Update look away status
        updateLookAwayStatus(headLookingAway)

        // Update blink detection
        updateBlinkDetection(leftEyeOpenProb, rightEyeOpenProb)

        // Update emotion detection
        updateEmotionDetection(face.smilingProbability ?: 0f)
    }

    private fun handleNoFace() {
        // Count as a look away if no face is detected
        if (!isLookingAway) {
            isLookingAway = true
            lastLookAwayStartTime = System.currentTimeMillis()
            lookAwayCount++
        }
    }

    private fun updateLookAwayStatus(lookingAway: Boolean) {
        val now = System.currentTimeMillis()

        if (lookingAway && !isLookingAway) {
            // Just started looking away
            isLookingAway = true
            lastLookAwayStartTime = now
            lookAwayCount++
        } else if (!lookingAway && isLookingAway) {
            // Just returned attention
            isLookingAway = false
            totalLookAwayTimeMs += (now - lastLookAwayStartTime)
        }
    }

    private fun updateBlinkDetection(leftEyeOpenProb: Float, rightEyeOpenProb: Float) {
        if (leftEyeOpenProb < 0 || rightEyeOpenProb < 0) return

        val leftEyeOpen = leftEyeOpenProb > 0.5f
        val rightEyeOpen = rightEyeOpenProb > 0.5f
        val now = System.currentTimeMillis()

        // Start of potential blink
        if ((lastLeftEyeOpen && lastRightEyeOpen) &&
            (!leftEyeOpen || !rightEyeOpen)) {
            potentialBlinkStartTime = now
            isInBlink = true
        }
        // End of blink (eyes reopened)
        else if (isInBlink &&
            (!lastLeftEyeOpen || !lastRightEyeOpen) &&
            (leftEyeOpen && rightEyeOpen)) {

            val blinkDuration = now - potentialBlinkStartTime
            isInBlink = false

            // Only count as blink if duration is reasonable (80-400ms)
            if (blinkDuration in 80..400) {
                blinkCount++
            }
        }

        lastLeftEyeOpen = leftEyeOpen
        lastRightEyeOpen = rightEyeOpen
    }

    private fun updateEmotionDetection(smileProbability: Float) {
        val currentEmotion = when {
            smileProbability > 0.8f -> "very happy"
            smileProbability > 0.5f -> "happy"
            smileProbability > 0.2f -> "slight smile"
            else -> "neutral"
        }

        if (currentEmotion != lastEmotion) {
            emotionChanges++
            lastEmotion = currentEmotion
        }
    }

    private fun updateMetrics() {
        val elapsedTime = System.currentTimeMillis() - startTime
        val faceVisiblePercentage = if (totalFrames > 0) (framesWithFace * 100) / totalFrames else 0
        val blinkRate = if (elapsedTime > 0) (blinkCount * 60000f) / elapsedTime else 0f

        _faceMetrics.value = FaceMetrics(
            lookAwayCount = lookAwayCount,
            attentionDurationMs = elapsedTime - totalLookAwayTimeMs,
            totalLookAwayTimeMs = totalLookAwayTimeMs,
            blinkCount = blinkCount,
            blinkRate = blinkRate,
            emotionChanges = emotionChanges,
            faceVisiblePercentage = faceVisiblePercentage
        )
    }

    fun getFinalMetrics(): FaceMetrics {
        updateMetrics()
        return _faceMetrics.value
    }
}