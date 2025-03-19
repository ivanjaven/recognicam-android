package com.example.recognicam.data.analysis

import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import kotlin.math.min

data class ADHDAssessmentResult(
    val adhdProbabilityScore: Int, // 0-100
    val attentionScore: Int, // 0-100
    val hyperactivityScore: Int, // 0-100
    val impulsivityScore: Int, // 0-100
    val confidenceLevel: Int, // 0-100
    val behavioralMarkers: List<BehavioralMarker>,
    val assessmentDuration: Long
)

data class BehavioralMarker(
    val name: String,
    val value: Float,
    val threshold: Float,
    val significance: Int, // 1-3 (mild, moderate, high)
    val description: String = "" // Added description for better context
)

class ADHDAnalyzer {

    fun analyzePerformance(
        correctResponses: Int,
        incorrectResponses: Int,
        missedResponses: Int,
        averageResponseTime: Int,
        responseTimeVariability: Float,
        faceMetrics: FaceMetrics,
        motionMetrics: MotionMetrics,
        durationSeconds: Int
    ): ADHDAssessmentResult {
        val markers = mutableListOf<BehavioralMarker>()

        // ===== Performance metrics =====
        val totalResponses = correctResponses + incorrectResponses + missedResponses
        val accuracy = if (totalResponses > 0) {
            (correctResponses * 100) / totalResponses
        } else {
            0
        }

        // Response time factors - ADJUSTED THRESHOLDS
        val responseTimeFactor = when {
            averageResponseTime > 700 -> 30
            averageResponseTime > 580 -> 25  // Was 600 - more sensitive
            averageResponseTime > 480 -> 15  // Was 500 - more sensitive
            averageResponseTime > 380 -> 5   // Was 400 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Response Time",
            value = averageResponseTime.toFloat(),
            threshold = 550f,
            significance = if (averageResponseTime > 650) 3 else if (averageResponseTime > 550) 2 else 1,
            description = "Average time to respond to target stimuli. Longer times may indicate processing delays."
        ))

        // Response time variability - ADJUSTED THRESHOLDS
        val variabilityFactor = when {
            responseTimeVariability > 210 -> 30  // Was 220 - more sensitive
            responseTimeVariability > 170 -> 20  // Was 180 - more sensitive
            responseTimeVariability > 120 -> 10  // Was 130 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Response Variability",
            value = responseTimeVariability,
            threshold = 180f,
            significance = if (responseTimeVariability > 220) 3 else if (responseTimeVariability > 180) 2 else 1,
            description = "Consistency of response timing. High variability is a key ADHD indicator."
        ))

        // Accuracy factors - ADJUSTED THRESHOLDS
        val accuracyFactor = when {
            accuracy < 60 -> 30  // Was 55 - more sensitive
            accuracy < 75 -> 20  // Was 70 - more sensitive
            accuracy < 85 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Task Accuracy",
            value = accuracy.toFloat(),
            threshold = 75f,
            significance = if (accuracy < 60) 3 else if (accuracy < 75) 2 else 1,
            description = "Percentage of correct responses. Lower accuracy may indicate attention difficulties."
        ))

        // Missed responses - ADJUSTED THRESHOLDS
        val missedResponseFactor = when {
            missedResponses > 12 -> 30  // Was 14 - more sensitive
            missedResponses > 7 -> 20   // Was 8 - more sensitive
            missedResponses > 3 -> 10   // Was 4 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Missed Responses",
            value = missedResponses.toFloat(),
            threshold = 8f,
            significance = if (missedResponses > 12) 3 else if (missedResponses > 7) 2 else 1,
            description = "Target stimuli that received no response. May indicate inattention."
        ))

        // ===== Face metrics =====
        val minuteMultiplier = 60f / durationSeconds

        // Look away count - ADJUSTED THRESHOLDS
        val lookAwayRate = faceMetrics.lookAwayCount * minuteMultiplier
        val lookAwayFactor = when {
            lookAwayRate > 10 -> 35  // Was 12 - more sensitive
            lookAwayRate > 7 -> 25   // Was 8 - more sensitive
            lookAwayRate > 4 -> 15
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Look Away Rate",
            value = lookAwayRate,
            threshold = 8f,
            significance = if (lookAwayRate > 10) 3 else if (lookAwayRate > 7) 2 else 1,
            description = "How often attention shifts away from the task. Natural to look away occasionally."
        ))

        // Sustained attention score - ADJUSTED THRESHOLDS
        val sustainedAttentionFactor = when {
            faceMetrics.sustainedAttentionScore < 30 -> 35  // Was 25 - more sensitive
            faceMetrics.sustainedAttentionScore < 45 -> 25  // Was 40 - more sensitive
            faceMetrics.sustainedAttentionScore < 60 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Sustained Attention",
            value = faceMetrics.sustainedAttentionScore.toFloat(),
            threshold = 50f,
            significance = if (faceMetrics.sustainedAttentionScore < 30) 3
            else if (faceMetrics.sustainedAttentionScore < 45) 2
            else 1,
            description = "Ability to maintain focus over time. Lower scores indicate difficulty maintaining attention."
        ))

        // Look away duration - ADJUSTED THRESHOLDS
        val lookAwayDurationFactor = when {
            faceMetrics.averageLookAwayDuration > 2400 -> 30  // Was 2500 - more sensitive
            faceMetrics.averageLookAwayDuration > 1900 -> 20  // Was 2000 - more sensitive
            faceMetrics.averageLookAwayDuration > 1400 -> 10  // Was 1500 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Look Away Duration",
            value = faceMetrics.averageLookAwayDuration,
            threshold = 2000f,
            significance = if (faceMetrics.averageLookAwayDuration > 2400) 3
            else if (faceMetrics.averageLookAwayDuration > 1900) 2
            else 1,
            description = "How long attention typically stays away from task when distracted."
        ))

        // Attention lapse frequency - ADJUSTED THRESHOLDS
        val attentionLapseFactor = when {
            faceMetrics.attentionLapseFrequency > 6 -> 25  // Was 7 - more sensitive
            faceMetrics.attentionLapseFrequency > 4 -> 15  // Was 5 - more sensitive
            faceMetrics.attentionLapseFrequency > 2 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Attention Lapses",
            value = faceMetrics.attentionLapseFrequency,
            threshold = 5f,
            significance = if (faceMetrics.attentionLapseFrequency > 6) 3
            else if (faceMetrics.attentionLapseFrequency > 4) 2
            else 1,
            description = "Moments when attention completely breaks from the task."
        ))

        // Distractibility index - ADJUSTED SCALING
        val scaledDistractibility = (faceMetrics.distractibilityIndex * 0.78f).toInt().coerceIn(0, 100)  // Was 0.75f
        val distractibilityFactor = when {
            scaledDistractibility > 80 -> 25  // Was 85 - more sensitive
            scaledDistractibility > 65 -> 15  // Was 70 - more sensitive
            scaledDistractibility > 45 -> 5   // Was 50 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Distractibility",
            value = scaledDistractibility.toFloat(),
            threshold = 70f,
            significance = if (scaledDistractibility > 80) 3
            else if (scaledDistractibility > 65) 2
            else 1,
            description = "Overall measure of how easily distracted. Some distractibility is normal."
        ))

        // Blink rate - ADJUSTED THRESHOLDS
        val normalizedBlinkRate = faceMetrics.blinkRate * 0.8f
        val blinkRateFactor = when {
            normalizedBlinkRate > 38 -> 15  // Was 40 - more sensitive
            normalizedBlinkRate > 32 -> 10  // Was 35 - more sensitive
            normalizedBlinkRate > 26 -> 5   // Was 30 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Blink Rate",
            value = normalizedBlinkRate,
            threshold = 35f,
            significance = if (normalizedBlinkRate > 38) 2 else 1,
            description = "Blinks per minute. Excessive blinking can indicate stress or hyperactivity."
        ))

        // Face visibility - SAME THRESHOLDS
        val faceVisibilityFactor = when {
            faceMetrics.faceVisiblePercentage < 60 -> 20
            faceMetrics.faceVisiblePercentage < 75 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Face Visibility",
            value = faceMetrics.faceVisiblePercentage.toFloat(),
            threshold = 75f,
            significance = if (faceMetrics.faceVisiblePercentage < 60) 2 else 1,
            description = "Percentage of time the face was visible to the camera."
        ))

        // Facial movement - ADJUSTED SCALING
        val scaledFacialMovement = (faceMetrics.facialMovementScore * 0.73f).toInt().coerceIn(0, 100)  // Was 0.7f
        val facialMovementFactor = when {
            scaledFacialMovement > 75 -> 20  // Was 80 - more sensitive
            scaledFacialMovement > 60 -> 10  // Was 65 - more sensitive
            scaledFacialMovement > 45 -> 5   // Was 50 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Facial Movement",
            value = scaledFacialMovement.toFloat(),
            threshold = 65f,
            significance = if (scaledFacialMovement > 75) 3
            else if (scaledFacialMovement > 60) 2
            else 1,
            description = "Amount of facial movement during task. Some movement is completely normal."
        ))

        // Emotion changes - ADJUSTED SCALING
        val scaledEmotionChanges = (faceMetrics.emotionChanges * 0.32f).coerceIn(0f, 30f)  // Was 0.3f
        val emotionChangeRate = scaledEmotionChanges * minuteMultiplier
        val emotionChangeFactor = when {
            emotionChangeRate > 7 -> 15  // Was 8 - more sensitive
            emotionChangeRate > 4 -> 10  // Was 5 - more sensitive
            emotionChangeRate > 2 -> 5   // Was 3 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Emotion Changes",
            value = emotionChangeRate,
            threshold = 5f,
            significance = if (emotionChangeRate > 7) 2 else 1,
            description = "Frequency of emotional expression changes. Rapid changes can indicate impulsivity."
        ))

        // Emotion variability - ADJUSTED THRESHOLDS
        val emotionVariabilityFactor = when {
            faceMetrics.emotionVariabilityScore > 75 -> 15  // Was 80 - more sensitive
            faceMetrics.emotionVariabilityScore > 60 -> 10  // Was 65 - more sensitive
            faceMetrics.emotionVariabilityScore > 45 -> 5   // Was 50 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Emotion Variability",
            value = faceMetrics.emotionVariabilityScore.toFloat(),
            threshold = 65f,
            significance = if (faceMetrics.emotionVariabilityScore > 75) 2
            else if (faceMetrics.emotionVariabilityScore > 60) 2
            else 1,
            description = "Intensity of emotional expression changes. Some variability is normal."
        ))

        // ===== Motion metrics =====
        // Fidgeting score - ADJUSTED THRESHOLDS
        val fidgetingFactor = when {
            motionMetrics.fidgetingScore > 75 -> 35  // Was 80 - more sensitive
            motionMetrics.fidgetingScore > 60 -> 25  // Was 65 - more sensitive
            motionMetrics.fidgetingScore > 40 -> 15  // Was 45 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Fidgeting Score",
            value = motionMetrics.fidgetingScore.toFloat(),
            threshold = 65f,
            significance = if (motionMetrics.fidgetingScore > 75) 3 else if (motionMetrics.fidgetingScore > 60) 2 else 1,
            description = "Small repeated movements. Some fidgeting is normal and not concerning."
        ))

        // Direction changes - ADJUSTED SCALING
        val scaledDirectionChanges = (motionMetrics.directionChanges * 0.22f).toInt()  // Was 0.2f
        val directionChangeRate = scaledDirectionChanges * minuteMultiplier
        val directionChangeFactor = when {
            directionChangeRate > 65 -> 20  // Was 70 - more sensitive
            directionChangeRate > 45 -> 15  // Was 50 - more sensitive
            directionChangeRate > 25 -> 5   // Was 30 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Direction Changes",
            value = directionChangeRate,
            threshold = 50f,
            significance = if (directionChangeRate > 65) 2 else if (directionChangeRate > 45) 2 else 1,
            description = "How often movement direction changes. Rapid shifts can indicate restlessness."
        ))

        // Sudden movements - ADJUSTED SCALING
        val scaledSuddenMovements = (motionMetrics.suddenMovements * 0.32f).toInt()  // Was 0.3f
        val suddenMovementRate = scaledSuddenMovements * minuteMultiplier
        val suddenMovementFactor = when {
            suddenMovementRate > 14 -> 20  // Was 15 - more sensitive
            suddenMovementRate > 9 -> 10   // Was 10 - more sensitive
            suddenMovementRate > 4 -> 5    // Was 5 - same
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Sudden Movements",
            value = suddenMovementRate,
            threshold = 10f,
            significance = if (suddenMovementRate > 14) 2 else 1,
            description = "Quick, unexpected movements. Can indicate impulsivity if frequent."
        ))

        // Restlessness - ADJUSTED THRESHOLDS
        val restlessnessFactor = when {
            motionMetrics.restlessness > 75 -> 25  // Was 80 - more sensitive
            motionMetrics.restlessness > 60 -> 15  // Was 65 - more sensitive
            motionMetrics.restlessness > 40 -> 5   // Was 45 - more sensitive
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Restlessness",
            value = motionMetrics.restlessness.toFloat(),
            threshold = 65f,
            significance = if (motionMetrics.restlessness > 75) 3 else if (motionMetrics.restlessness > 60) 2 else 1,
            description = "Overall physical activity level. Some movement during tasks is completely normal."
        ))

        // ADJUSTED DOMAIN SCORE CALCULATIONS with slightly increased weights
        // Attention score with improved weighting
        val attentionScore = min(100, lookAwayFactor + (missedResponseFactor * 1.6).toInt() +
                (faceVisibilityFactor / 2) + (sustainedAttentionFactor * 1.2).toInt() +
                (lookAwayDurationFactor / 2) + attentionLapseFactor + (accuracyFactor * 1.0).toInt() +
                (responseTimeFactor / 3))

        // Hyperactivity score with improved weighting
        val hyperactivityScore = min(100, (fidgetingFactor * 1.25).toInt() + (restlessnessFactor * 1.1).toInt() +
                (facialMovementFactor / 2) + blinkRateFactor + (directionChangeFactor / 2))

        // Impulsivity score with improved weighting
        val impulsivityScore = min(100, (emotionChangeFactor * 1.1).toInt() + (suddenMovementFactor * 1.1).toInt() +
                (emotionVariabilityFactor / 2) + (distractibilityFactor / 2) +
                (incorrectResponses * 2.1).toInt() + (variabilityFactor / 3))

        // Apply a SMALLER BASELINE ADJUSTMENT to prevent underestimation (down from 7% to 5%)
        val baselineAdjustment = 5

        val adjustedAttentionScore = (attentionScore - baselineAdjustment).coerceIn(0, 100)
        val adjustedHyperactivityScore = (hyperactivityScore - baselineAdjustment).coerceIn(0, 100)
        val adjustedImpulsivityScore = (impulsivityScore - baselineAdjustment).coerceIn(0, 100)

        // Overall ADHD probability calculation with ADJUSTED domain weighting
        val adhdProbabilityScore = min(100, (adjustedAttentionScore * 0.47 +  // Was 0.45 - increased weight of attention
                adjustedHyperactivityScore * 0.33 +  // Was 0.35 - decreased weight of hyperactivity
                adjustedImpulsivityScore * 0.2).toInt())

        // Calculate confidence level based on data quality
        val confidenceLevel = calculateConfidenceLevel(
            faceMetrics.faceVisiblePercentage,
            durationSeconds,
            markers.size
        )

        return ADHDAssessmentResult(
            adhdProbabilityScore = adhdProbabilityScore,
            attentionScore = adjustedAttentionScore,
            hyperactivityScore = adjustedHyperactivityScore,
            impulsivityScore = adjustedImpulsivityScore,
            confidenceLevel = confidenceLevel,
            behavioralMarkers = markers,
            assessmentDuration = durationSeconds * 1000L
        )
    }

    private fun calculateConfidenceLevel(
        faceVisibility: Int,
        duration: Int,
        markerCount: Int
    ): Int {
        // Base confidence on data quality
        var confidence = 70 // Start with reasonable baseline

        // Duration affects confidence (longer is better)
        confidence += when {
            duration >= 120 -> 15
            duration >= 60 -> 10
            duration >= 30 -> 5
            else -> 0
        }

        // Face visibility affects confidence
        confidence += when {
            faceVisibility > 95 -> 15
            faceVisibility > 85 -> 10
            faceVisibility > 75 -> 5
            else -> -10
        }

        // More behavioral markers increases confidence
        confidence += when {
            markerCount >= 15 -> 15
            markerCount >= 10 -> 10
            markerCount >= 8 -> 5
            markerCount >= 6 -> 0
            else -> -10
        }

        return confidence.coerceIn(0, 100)
    }
}