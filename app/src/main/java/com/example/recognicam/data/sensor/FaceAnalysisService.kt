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
import kotlin.math.sqrt

data class FaceMetrics(
    val lookAwayCount: Int = 0,
    val attentionDurationMs: Long = 0,
    val totalLookAwayTimeMs: Long = 0,
    val blinkCount: Int = 0,
    val blinkRate: Float = 0f,
    val emotionChanges: Int = 0,
    val faceVisiblePercentage: Int = 0,
    // New research-backed metrics for ADHD assessment
    val averageLookAwayDuration: Float = 0f,         // Average time looking away
    val facialMovementScore: Int = 0,                // Score of facial restlessness
    val emotionVariabilityScore: Int = 0,            // Emotional variability score
    val attentionLapseFrequency: Float = 0f,         // Attention lapses per minute
    val focusRecoveryTime: Float = 0f,               // Recovery time after distraction
    val sustainedAttentionScore: Int = 0,            // Ability to maintain attention
    val distractibilityIndex: Int = 0                // Measure of distractibility
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

    // Basic metrics tracking
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
    private var lastEmotionChangeTime = 0L

    // Blink detection
    private var lastLeftEyeOpen = true
    private var lastRightEyeOpen = true
    private var potentialBlinkStartTime = 0L
    private var isInBlink = false

    // Advanced tracking for research-backed metrics
    private var totalLookAwayDuration = 0L
    private var facialMovementCount = 0
    private var emotionIntensitySum = 0f
    private var emotionSamples = 0
    private var lastEmotionIntensity = 0f
    private var attentionLapses = 0
    private var attentionRecoveryTimes = mutableListOf<Long>()
    private var consecutiveAttentionFrames = 0
    private var longestAttentionStreak = 0
    private val facePositions = mutableListOf<Pair<Float, Float>>()
    private var totalAttentionDuration = 0L
    private var distractibilityEvents = 0

    // Limit events to avoid skewed metrics
    private val MAX_DISTRACTIBILITY_EVENTS = 30
    private val MAX_EMOTION_CHANGES = 15
    private val MAX_FACIAL_MOVEMENT = 40

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
        lastEmotionChangeTime = 0L

        // Reset advanced metrics
        totalLookAwayDuration = 0L
        facialMovementCount = 0
        emotionIntensitySum = 0f
        emotionSamples = 0
        lastEmotionIntensity = 0f
        attentionLapses = 0
        attentionRecoveryTimes.clear()
        consecutiveAttentionFrames = 0
        longestAttentionStreak = 0
        facePositions.clear()
        totalAttentionDuration = 0L
        distractibilityEvents = 0

        _faceMetrics.value = FaceMetrics()

        // Debug logging
        println("Face analysis started")
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
                    handleNoFace()
                    updateMetrics()
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

        // Track face position for movement analysis
        val centerX = face.boundingBox.centerX().toFloat()
        val centerY = face.boundingBox.centerY().toFloat()
        facePositions.add(Pair(centerX, centerY))

        // Limit position history to prevent memory issues
        if (facePositions.size > 100) {
            facePositions.removeAt(0)
        }

        // Analyze facial movement if we have history
        if (facePositions.size > 2) {
            analyzeFacialMovement()
        }

        // Update attention streak
        if (!isLookingAway) {
            consecutiveAttentionFrames++
            longestAttentionStreak = maxOf(longestAttentionStreak, consecutiveAttentionFrames)
        } else {
            consecutiveAttentionFrames = 0
        }

        // Process head position for attention - ADJUSTED THRESHOLDS
        val rotY = face.headEulerAngleY // Head rotation (left/right)
        val rotX = face.headEulerAngleX // Head rotation (up/down)

        // Detect look away with improved sensitivity
        val headLookingAway = abs(rotY) > 36f || abs(rotX) > 31f // Balanced thresholds for normal vs excessive movement

        // Process eye states for blink detection
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: -1f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: -1f

        // Update look away status
        updateLookAwayStatus(headLookingAway)

        // Update blink detection
        updateBlinkDetection(leftEyeOpenProb, rightEyeOpenProb)

        // Enhanced emotion detection
        updateEmotionDetection(face.smilingProbability ?: 0f)
    }

    private fun analyzeFacialMovement() {
        // Calculate movements between consecutive frames
        val last = facePositions[facePositions.size - 1]
        val secondLast = facePositions[facePositions.size - 2]

        val moveX = abs(last.first - secondLast.first)
        val moveY = abs(last.second - secondLast.second)
        val movement = sqrt(moveX * moveX + moveY * moveY)

        // Consider it significant movement if above threshold
        val MOVEMENT_THRESHOLD = 9.5f  // Balanced threshold for normal movement
        if (movement > MOVEMENT_THRESHOLD) {
            // Only increment if below maximum to prevent inflation
            if (facialMovementCount < MAX_FACIAL_MOVEMENT) {
                facialMovementCount++
            }

            // Large movements can indicate distractibility
            if (movement > MOVEMENT_THRESHOLD * 4 && distractibilityEvents < MAX_DISTRACTIBILITY_EVENTS) {
                distractibilityEvents++
            }
        }
    }

    private fun handleNoFace() {
        // Count as a look away if no face is detected
        if (!isLookingAway) {
            isLookingAway = true
            lastLookAwayStartTime = System.currentTimeMillis()
            lookAwayCount++
            attentionLapses++
            consecutiveAttentionFrames = 0

            // Debug logging
            println("Look away detected: $lookAwayCount")
        }
    }

    private fun updateLookAwayStatus(lookingAway: Boolean) {
        val now = System.currentTimeMillis()

        if (lookingAway && !isLookingAway) {
            // Just started looking away
            isLookingAway = true
            lastLookAwayStartTime = now
            lookAwayCount++
            attentionLapses++
            consecutiveAttentionFrames = 0

            // Debug logging
            println("Look away detected: $lookAwayCount")
        } else if (!lookingAway && isLookingAway) {
            // Just returned attention
            isLookingAway = false
            val lookAwayDuration = now - lastLookAwayStartTime
            totalLookAwayTimeMs += lookAwayDuration
            totalLookAwayDuration += lookAwayDuration

            // Track recovery time for analysis
            attentionRecoveryTimes.add(lookAwayDuration)
        }
    }

    private fun updateBlinkDetection(leftEyeOpenProb: Float, rightEyeOpenProb: Float) {
        if (leftEyeOpenProb < 0 || rightEyeOpenProb < 0) return

        val leftEyeOpen = leftEyeOpenProb > 0.5f  // Balanced threshold
        val rightEyeOpen = rightEyeOpenProb > 0.5f  // Balanced threshold
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

            // Only count as blink if duration is reasonable
            if (blinkDuration in 80..400 && (now - lastBlinkTime > 300)) {
                blinkCount++
                lastBlinkTime = now

                // Rapid blinking can be linked to ADHD (but limit influence)
                if (blinkDuration < 150 && distractibilityEvents < MAX_DISTRACTIBILITY_EVENTS) {
                    distractibilityEvents++
                }

                // Debug logging
                if (blinkCount % 5 == 0) {
                    println("Blink count: $blinkCount")
                }
            }
        }

        lastLeftEyeOpen = leftEyeOpen
        lastRightEyeOpen = rightEyeOpen
    }

    // Add this variable near other blink variables
    private var lastBlinkTime = 0L

    private fun updateEmotionDetection(smileProbability: Float) {
        // Track emotional intensity (using smile probability as proxy)
        emotionSamples++
        emotionIntensitySum += smileProbability

        // Balanced emotion detection sensitivity
        val emotionDelta = abs(smileProbability - lastEmotionIntensity)
        if (emotionDelta > 0.4f) {
            // Only increment if below maximum
            if (emotionChanges < MAX_EMOTION_CHANGES) {
                emotionChanges++
            }
        }

        lastEmotionIntensity = smileProbability

        // Enhanced emotion classifications
        val currentEmotion = when {
            smileProbability > 0.8f -> "very happy"
            smileProbability > 0.5f -> "happy"
            smileProbability > 0.2f -> "slight smile"
            else -> "neutral"
        }

        if (currentEmotion != lastEmotion) {
            // Don't count every emotion change, use balanced time threshold
            if (System.currentTimeMillis() - lastEmotionChangeTime > 1500) {
                // Only increment if below maximum
                if (emotionChanges < MAX_EMOTION_CHANGES) {
                    emotionChanges++
                }
                lastEmotion = currentEmotion
                lastEmotionChangeTime = System.currentTimeMillis()
            }

            // Emotional instability can be a sign of ADHD (with limits)
            if (emotionDelta > 0.6f && distractibilityEvents < MAX_DISTRACTIBILITY_EVENTS) {
                distractibilityEvents++
            }
        }
    }

    private fun updateMetrics() {
        val elapsedTime = System.currentTimeMillis() - startTime
        val faceVisiblePercentage = if (totalFrames > 0) (framesWithFace * 100) / totalFrames else 0

        // Scale down blink rate to avoid overestimation
        val blinkRate = if (elapsedTime > 0) (blinkCount * 60000f * 0.8f) / elapsedTime else 0f

        // Calculate sustained attention based on longest streak
        val sustainedAttentionScore = (longestAttentionStreak * 100f / (totalFrames.coerceAtLeast(1))).toInt().coerceIn(0, 100)

        // Calculate enhanced metrics
        val avgLookAwayDuration = if (lookAwayCount > 0)
            totalLookAwayDuration.toFloat() / lookAwayCount else 0f

        // Scale facial movement for a more realistic score
        val facialMovementScore = if (elapsedTime > 0) {
            val baseScore = ((facialMovementCount * 60000f) / elapsedTime)
            (baseScore * 0.65f).toInt().coerceIn(0, 100)  // Balanced scaling
        } else 0

        // Scale emotion variability for more realistic scores
        val emotionVariabilityScore = if (emotionSamples > 0) {
            val baseScore = ((emotionChanges * 100f) / emotionSamples)
            (baseScore * 0.7f).toInt().coerceIn(0, 100)  // Balanced scaling
        } else 0

        val attentionLapseFrequency = if (elapsedTime > 0)
            (attentionLapses * 60000f) / elapsedTime else 0f

        val focusRecoveryTime = if (attentionRecoveryTimes.isNotEmpty())
            attentionRecoveryTimes.average().toFloat() else 0f

        // IMPROVED DISTRACTIBILITY CALCULATION
        val distractibilityIndex = if (elapsedTime > 0) {
            // Start with the inverse of sustained attention
            val baseDistractibility = 100 - sustainedAttentionScore

            // Add influence from distractibility events with increased weight
            val eventFactor = (distractibilityEvents * 18f) / MAX_DISTRACTIBILITY_EVENTS  // Was 17f

            // Factor in look away rate with increased influence
            val lookAwayFactor = if (elapsedTime > 0) {
                val lookAwaysPerMinute = (lookAwayCount * 60000f) / elapsedTime
                (lookAwaysPerMinute * 3.8f).coerceIn(0f, 38f)  // Was 3.5f/35f
            } else 0f

            // Final calculation with adjusted weights
            val combinedScore = (baseDistractibility * 0.5f) +
                    (eventFactor * 0.3f) +
                    (lookAwayFactor * 0.2f)

            // Remove scaling factor that was causing underestimation
            combinedScore.toInt().coerceIn(0, 100)  // Removed 0.95f scaling
        } else 0

        _faceMetrics.value = FaceMetrics(
            lookAwayCount = lookAwayCount,
            attentionDurationMs = elapsedTime - totalLookAwayTimeMs,
            totalLookAwayTimeMs = totalLookAwayTimeMs,
            blinkCount = blinkCount,
            blinkRate = blinkRate,
            emotionChanges = emotionChanges,
            faceVisiblePercentage = faceVisiblePercentage,
            // Enhanced metrics
            averageLookAwayDuration = avgLookAwayDuration,
            facialMovementScore = facialMovementScore,
            emotionVariabilityScore = emotionVariabilityScore,
            attentionLapseFrequency = attentionLapseFrequency,
            focusRecoveryTime = focusRecoveryTime,
            sustainedAttentionScore = sustainedAttentionScore,
            distractibilityIndex = distractibilityIndex
        )
    }

    fun getFinalMetrics(): FaceMetrics {
        updateMetrics()
        // Log final metrics
        println("Final face metrics - Look aways: ${_faceMetrics.value.lookAwayCount}, " +
                "Blinks: ${_faceMetrics.value.blinkCount}, " +
                "Distractibility: ${_faceMetrics.value.distractibilityIndex}%")
        return _faceMetrics.value
    }
}