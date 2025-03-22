package com.example.recognicam.data.sensor

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
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
    val lookAwayCount: Int = 0,                    // Count of times gaze shifted away
    val attentionDurationMs: Long = 0,             // Time with attention focused
    val totalLookAwayTimeMs: Long = 0,             // Total time looking away
    val blinkCount: Int = 0,                       // Count of blinks
    val blinkRate: Float = 0f,                     // Blinks per minute
    val emotionChanges: Int = 0,                   // Count of emotion shifts
    val faceVisiblePercentage: Int = 0,            // % time face was visible
    val averageLookAwayDuration: Float = 0f,       // Average duration of look-aways
    val facialMovementScore: Int = 0,              // Score of facial movement/restlessness
    val emotionVariabilityScore: Int = 0,          // Emotional variability score
    val attentionLapseFrequency: Float = 0f,       // Attention lapses per minute
    val focusRecoveryTime: Float = 0f,             // Recovery time after distraction
    val sustainedAttentionScore: Int = 0,          // Ability to maintain attention (0-100)
    val distractibilityIndex: Int = 0              // Overall distractibility (0-100)
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
    private var lastBlinkTime = 0L

    // Advanced tracking for research-backed metrics
    private var totalLookAwayDuration = 0L
    private var lookAwayDurations = mutableListOf<Long>()
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
    private var lastHeadPosition = Triple(0f, 0f, 0f) // yaw, pitch, roll
    private var headMovementScore = 0

    // Face landmarks and expressions tracking
    private var expressionVariability = 0f
    private var lastExpressionTime = 0L
    private var expressionChanges = 0
    private var expressionIntensities = mutableListOf<Float>()

    // Enhanced emotion tracking
    private var emotionHistory = mutableListOf<String>()
    private var mouthOpennessFactor = 0f
    private var lastMouthOpennessTime = 0L
    private var yawnDetected = false
    private var confusionDetected = false

    // Limit events to avoid skewed metrics
    private val MAX_DISTRACTIBILITY_EVENTS = 40
    private val MAX_EMOTION_CHANGES = 40  // Increased from 25 to better capture variations
    private val MAX_FACIAL_MOVEMENT = 60

    // Improved attention assessment
    private var attentionQuality = mutableListOf<Float>() // Track quality scores over time
    private var framesSinceLastFacialMovement = 0

    // State flow for real-time updates
    private val _faceMetrics = MutableStateFlow(FaceMetrics())
    val faceMetrics: StateFlow<FaceMetrics> = _faceMetrics.asStateFlow()

    fun start() {
        startTime = System.currentTimeMillis()
        resetCounters()
        _faceMetrics.value = FaceMetrics()
        println("Face analysis started")
    }

    private fun resetCounters() {
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
        lastBlinkTime = 0L
        totalLookAwayDuration = 0L
        lookAwayDurations.clear()
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
        lastHeadPosition = Triple(0f, 0f, 0f)
        headMovementScore = 0
        expressionVariability = 0f
        lastExpressionTime = 0L
        expressionChanges = 0
        expressionIntensities.clear()
        attentionQuality.clear()
        framesSinceLastFacialMovement = 0
        emotionHistory.clear()
        mouthOpennessFactor = 0f
        lastMouthOpennessTime = 0L
        yawnDetected = false
        confusionDetected = false
    }

    fun reset() {
        start()
    }

    @OptIn(ExperimentalGetImage::class)
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

        // Track head position
        val newHeadPosition = Triple(
            face.headEulerAngleY, // yaw (left/right)
            face.headEulerAngleX, // pitch (up/down)
            face.headEulerAngleZ  // roll (tilt)
        )

        // Calculate head movement
        val headMovement = calculateHeadMovement(lastHeadPosition, newHeadPosition)
        if (headMovement > 5f) { // Threshold for significant head movement
            headMovementScore = (headMovementScore + 1).coerceAtMost(100)
            framesSinceLastFacialMovement = 0
        } else {
            framesSinceLastFacialMovement++
            // Gradually decrease head movement score if no movement
            if (framesSinceLastFacialMovement > 30 && headMovementScore > 0) {
                headMovementScore = (headMovementScore - 1).coerceAtLeast(0)
            }
        }

        lastHeadPosition = newHeadPosition

        // Analyze facial movement if we have history
        if (facePositions.size > 2) {
            analyzeFacialMovement()
        }

        // Update attention streak
        if (!isLookingAway) {
            consecutiveAttentionFrames++
            longestAttentionStreak = maxOf(longestAttentionStreak, consecutiveAttentionFrames)

            // Add attention quality score (1.0 is perfect attention)
            val attentionQualityScore = calculateAttentionQualityScore(face)
            attentionQuality.add(attentionQualityScore)
        } else {
            consecutiveAttentionFrames = 0
            attentionQuality.add(0f) // No attention
        }

        // Process head position for attention - Adjusted thresholds
        val rotY = abs(face.headEulerAngleY) // Head rotation (left/right)
        val rotX = abs(face.headEulerAngleX) // Head rotation (up/down)

        // Detect look away with improved sensitivity
        val headLookingAway = rotY > 30f || rotX > 25f

        // Process eye states for blink detection
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: -1f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: -1f

        // Update look away status
        updateLookAwayStatus(headLookingAway)

        // Update blink detection
        updateBlinkDetection(leftEyeOpenProb, rightEyeOpenProb)

        // Enhanced emotion detection
        updateEmotionDetection(face)
    }

    private fun calculateHeadMovement(
        previous: Triple<Float, Float, Float>,
        current: Triple<Float, Float, Float>
    ): Float {
        // Calculate Euclidean distance for head rotation
        return sqrt(
            (current.first - previous.first).pow(2) +
                    (current.second - previous.second).pow(2) +
                    (current.third - previous.third).pow(2)
        )
    }

    private fun Float.pow(n: Int): Float {
        var result = 1f
        repeat(n) { result *= this }
        return result
    }

    private fun calculateAttentionQualityScore(face: Face): Float {
        // Multi-factor attention quality score
        // 1. Eye openness
        val eyeOpenness = ((face.leftEyeOpenProbability ?: 0.5f) + (face.rightEyeOpenProbability ?: 0.5f)) / 2f

        // 2. Head pose (0 is forward-facing, higher values are looking away)
        val headPoseYaw = abs(face.headEulerAngleY) / 45f // Normalize to 0-1
        val headPosePitch = abs(face.headEulerAngleX) / 30f // Normalize to 0-1
        val headPoseScore = 1f - ((headPoseYaw + headPosePitch) / 2f).coerceIn(0f, 1f)

        // 3. Facial expression engagement (smile can indicate engagement but not always)
        val expressionScore = if ((face.smilingProbability ?: 0f) > 0.7f) 0.9f else 1.0f

        // Final quality score (weighted average)
        return (eyeOpenness * 0.4f + headPoseScore * 0.5f + expressionScore * 0.1f).coerceIn(0f, 1f)
    }

    private fun analyzeFacialMovement() {
        // Calculate movements between consecutive frames
        val last = facePositions[facePositions.size - 1]
        val secondLast = facePositions[facePositions.size - 2]

        val moveX = abs(last.first - secondLast.first)
        val moveY = abs(last.second - secondLast.second)
        val movement = sqrt(moveX * moveX + moveY * moveY)

        // Consider it significant movement if above threshold
        val MOVEMENT_THRESHOLD = 8.0f
        if (movement > MOVEMENT_THRESHOLD) {
            // Only increment if below maximum to prevent inflation
            if (facialMovementCount < MAX_FACIAL_MOVEMENT) {
                facialMovementCount++
            }

            // Large movements can indicate distractibility
            if (movement > MOVEMENT_THRESHOLD * 3 && distractibilityEvents < MAX_DISTRACTIBILITY_EVENTS) {
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

            // Track attention quality (none when face is not visible)
            attentionQuality.add(0f)

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
            lookAwayDurations.add(lookAwayDuration)

            // Track recovery time for analysis
            attentionRecoveryTimes.add(lookAwayDuration)
        }
    }

    private fun updateBlinkDetection(leftEyeOpenProb: Float, rightEyeOpenProb: Float) {
        if (leftEyeOpenProb < 0 || rightEyeOpenProb < 0) return

        val leftEyeOpen = leftEyeOpenProb > 0.3f
        val rightEyeOpen = rightEyeOpenProb > 0.3f
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
            if (blinkDuration in 50..400 && (now - lastBlinkTime > 250)) {
                blinkCount++
                lastBlinkTime = now

                // Rapid blinking can be linked to ADHD
                if (blinkDuration < 120 && distractibilityEvents < MAX_DISTRACTIBILITY_EVENTS) {
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

    private fun updateEmotionDetection(face: Face) {
        val now = System.currentTimeMillis()
        val smileProbability = face.smilingProbability ?: 0f
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0.5f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0.5f
        val avgEyeOpenness = (leftEyeOpenProb + rightEyeOpenProb) / 2f

        // Track emotional intensity (using smile probability as proxy)
        emotionSamples++
        emotionIntensitySum += smileProbability

        // Store emotion intensities for variability calculation
        expressionIntensities.add(smileProbability)

        // Limit history to prevent memory issues
        if (expressionIntensities.size > 100) {
            expressionIntensities.removeFirst()
        }

        // IMPROVED: Lower threshold for emotion delta detection (0.35f -> 0.2f)
        val emotionDelta = abs(smileProbability - lastEmotionIntensity)
        if (emotionDelta > 0.2f) {
            // Only increment if below maximum
            if (emotionChanges < MAX_EMOTION_CHANGES) {
                emotionChanges++
            }

            // Track expressions changes (large changes add to distractibility)
            // IMPROVED: Lower threshold and time requirement (0.5f -> 0.3f, 1000ms -> 800ms)
            if (emotionDelta > 0.3f && now - lastExpressionTime > 800) {
                expressionChanges++
                lastExpressionTime = now

                // Calculate expression variability (std deviation-like)
                if (expressionIntensities.size > 5) {
                    val mean = expressionIntensities.average().toFloat()
                    val variance = expressionIntensities.map { (it - mean) * (it - mean) }.sum() / expressionIntensities.size
                    expressionVariability = sqrt(variance)
                }
            }
        }

        lastEmotionIntensity = smileProbability

        // IMPROVED: Detect yawning using eye openness and head position
        val isYawning = detectYawning(face, avgEyeOpenness)
        if (isYawning && !yawnDetected) {
            yawnDetected = true
            if (emotionChanges < MAX_EMOTION_CHANGES && now - lastEmotionChangeTime > 800) {
                emotionChanges += 2 // Count yawning as a significant emotion change
                lastEmotionChangeTime = now
            }
        } else if (!isYawning && yawnDetected) {
            yawnDetected = false
        }

        // IMPROVED: Detect confusion using head tilt and eye openness
        val isConfused = detectConfusion(face, avgEyeOpenness)
        if (isConfused && !confusionDetected) {
            confusionDetected = true
            if (emotionChanges < MAX_EMOTION_CHANGES && now - lastEmotionChangeTime > 800) {
                emotionChanges++
                lastEmotionChangeTime = now
            }
        } else if (!isConfused && confusionDetected) {
            confusionDetected = false
        }

        // IMPROVED: Enhanced emotion classifications with more states
        val currentEmotion = when {
            isYawning -> "yawning"
            isConfused -> "confused"
            smileProbability > 0.8f -> "very happy"
            smileProbability > 0.5f -> "happy"
            smileProbability > 0.2f -> "slight smile"
            avgEyeOpenness < 0.3f -> "tired"
            face.headEulerAngleX > 15f && smileProbability < 0.2f -> "downcast"
            face.headEulerAngleX < -10f -> "looking up"
            abs(face.headEulerAngleZ) > 15f -> "tilted head"
            else -> "neutral"
        }

        // IMPROVED: Reduced time threshold for emotion changes (1500ms -> 800ms)
        if (currentEmotion != lastEmotion) {
            emotionHistory.add(currentEmotion)
            if (emotionHistory.size > 10) emotionHistory.removeAt(0)

            // Don't count every emotion change, use shorter time threshold
            if (now - lastEmotionChangeTime > 800) {
                // Only increment if below maximum
                if (emotionChanges < MAX_EMOTION_CHANGES) {
                    emotionChanges++
                }
                lastEmotion = currentEmotion
                lastEmotionChangeTime = now
            }

            // Emotional instability can be a sign of ADHD (with limits)
            // IMPROVED: Lower threshold for distractibility events (0.6f -> 0.4f)
            if (emotionDelta > 0.4f && distractibilityEvents < MAX_DISTRACTIBILITY_EVENTS) {
                distractibilityEvents++
            }
        }
    }

    // ADDED: Detect yawning based on available face features
    private fun detectYawning(face: Face, avgEyeOpenness: Float): Boolean {
        // Yawning often involves:
        // 1. Partially closed eyes
        // 2. Head tilted slightly back
        // 3. Low smile probability
        val smileProb = face.smilingProbability ?: 0f
        val headTiltBack = face.headEulerAngleX < -5f  // looking up slightly

        // Basic yawn heuristic - works with available ML Kit face data
        return avgEyeOpenness < 0.4f && headTiltBack && smileProb < 0.2f
    }

    // ADDED: Detect confusion based on available face features
    private fun detectConfusion(face: Face, avgEyeOpenness: Float): Boolean {
        // Confusion often involves:
        // 1. Head tilt (roll axis)
        // 2. Wider eyes
        // 3. Low smile probability
        val headTilt = abs(face.headEulerAngleZ) > 12f  // head tilted sideways
        val smileProb = face.smilingProbability ?: 0f

        return headTilt && avgEyeOpenness > 0.6f && smileProb < 0.3f
    }

    private fun updateMetrics() {
        val elapsedTime = System.currentTimeMillis() - startTime
        val faceVisiblePercentage = if (totalFrames > 0) (framesWithFace * 100) / totalFrames else 0

        // Calculate blink rate (blinks per minute)
        val blinkRate = if (elapsedTime > 0) (blinkCount * 60000f) / elapsedTime else 0f

        // Calculate sustained attention score with improved algorithm
        val attentionStreakScore = (longestAttentionStreak * 100f / (totalFrames.coerceAtLeast(1) * 0.7f)).coerceIn(0f, 100f)

        // Average attention quality score (when face was visible)
        val avgAttentionQuality = if (attentionQuality.isNotEmpty()) {
            attentionQuality.average().toFloat() * 100
        } else 50f

        // Combine streak and quality for overall sustained attention score
        val sustainedAttentionScore = ((attentionStreakScore * 0.6f) + (avgAttentionQuality * 0.4f)).toInt().coerceIn(0, 100)

        // Calculate enhanced metrics
        val avgLookAwayDuration = if (lookAwayDurations.isNotEmpty()) {
            lookAwayDurations.average().toFloat()
        } else 0f

        // Scale facial movement for a more realistic score
        val facialMovementScore = if (elapsedTime > 0) {
            val baseScore = ((facialMovementCount * 60000f) / elapsedTime) + (headMovementScore * 0.3f)
            (baseScore * 0.7f).toInt().coerceIn(0, 100)
        } else 0

        // Calculate emotion variability - IMPROVED calculation
        val emotionVariabilityScore = if (emotionSamples > 0) {
            // Base calculation from expression changes
            val baseVariability = ((emotionChanges * 100f) / emotionSamples.coerceAtLeast(1))
            // Add weight from expression variability
            val adjustedVariability = baseVariability + (expressionVariability * 50f)
            // Add unique emotion types factor
            val uniqueEmotionsFactor = emotionHistory.distinct().size * 5f

            // Combine factors with weights
            ((adjustedVariability * 0.7f) + (uniqueEmotionsFactor * 0.3f)).toInt().coerceIn(0, 100)
        } else 0

        // Calculate attention lapse frequency (per minute)
        val attentionLapseFrequency = if (elapsedTime > 0) {
            (attentionLapses * 60000f) / elapsedTime
        } else 0f

        // Calculate focus recovery time
        val focusRecoveryTime = if (attentionRecoveryTimes.isNotEmpty()) {
            attentionRecoveryTimes.average().toFloat()
        } else 0f

        // Calculate distractibility index with improved formula
        val distractibilityIndex = if (elapsedTime > 0) {
            // Start with inverse of sustained attention
            val baseDistractibility = 100 - sustainedAttentionScore

            // Add influence from distractibility events
            val eventFactor = (distractibilityEvents * 20f) / MAX_DISTRACTIBILITY_EVENTS.coerceAtLeast(1)

            // Factor in look away rate
            val lookAwayFactor = if (elapsedTime > 0) {
                val lookAwaysPerMinute = (lookAwayCount * 60000f) / elapsedTime
                (lookAwaysPerMinute * 4f).coerceIn(0f, 40f)
            } else 0f

            // Final calculation with adjusted weights
            ((baseDistractibility * 0.5f) +
                    (eventFactor * 0.3f) +
                    (lookAwayFactor * 0.2f)).toInt().coerceIn(0, 100)
        } else 0

        _faceMetrics.value = FaceMetrics(
            lookAwayCount = lookAwayCount,
            attentionDurationMs = elapsedTime - totalLookAwayTimeMs,
            totalLookAwayTimeMs = totalLookAwayTimeMs,
            blinkCount = blinkCount,
            blinkRate = blinkRate,
            emotionChanges = emotionChanges,
            faceVisiblePercentage = faceVisiblePercentage,
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
        println("Final face metrics - Look aways: ${_faceMetrics.value.lookAwayCount}, " +
                "Sustained attention: ${_faceMetrics.value.sustainedAttentionScore}%, " +
                "Distractibility: ${_faceMetrics.value.distractibilityIndex}%, " +
                "Emotion changes: ${_faceMetrics.value.emotionChanges}")
        return _faceMetrics.value
    }
}