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

        // Response time factors - normalized to 0-100 scale for consistency
        val responseTimeFactor = when {
            averageResponseTime > 800 -> 100
            averageResponseTime > 700 -> 85
            averageResponseTime > 600 -> 70
            averageResponseTime > 500 -> 55
            averageResponseTime > 400 -> 40
            averageResponseTime > 300 -> 25
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Response Time",
            value = averageResponseTime.toFloat(),
            threshold = 550f,
            significance = if (averageResponseTime > 650) 3 else if (averageResponseTime > 550) 2 else 1,
            description = "Average time to respond to target stimuli. Longer times may indicate processing delays."
        ))

        // Response time variability - normalized to 0-100 scale
        val normalizedVariability = (responseTimeVariability / 3).coerceIn(0f, 100f)
        val variabilityFactor = when {
            normalizedVariability > 80 -> 100
            normalizedVariability > 65 -> 80
            normalizedVariability > 50 -> 60
            normalizedVariability > 35 -> 40
            normalizedVariability > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Response Variability",
            value = responseTimeVariability,
            threshold = 180f,
            significance = if (responseTimeVariability > 220) 3 else if (responseTimeVariability > 180) 2 else 1,
            description = "Consistency of response timing. High variability is a key ADHD indicator."
        ))

        // Accuracy factors - already on 0-100 scale
        val accuracyFactor = when {
            accuracy < 50 -> 100
            accuracy < 65 -> 80
            accuracy < 75 -> 60
            accuracy < 85 -> 40
            accuracy < 95 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Task Accuracy",
            value = accuracy.toFloat(),
            threshold = 75f,
            significance = if (accuracy < 60) 3 else if (accuracy < 75) 2 else 1,
            description = "Percentage of correct responses. Lower accuracy may indicate attention difficulties."
        ))

        // Missed responses - normalize to 0-100 scale
        val missedResponseRate = if (totalResponses > 0) {
            (missedResponses * 100) / totalResponses
        } else 0

        val missedResponseFactor = when {
            missedResponseRate > 25 -> 100
            missedResponseRate > 20 -> 80
            missedResponseRate > 15 -> 60
            missedResponseRate > 10 -> 40
            missedResponseRate > 5 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Missed Responses",
            value = missedResponseRate.toFloat(),
            threshold = 15f,
            significance = if (missedResponseRate > 20) 3 else if (missedResponseRate > 10) 2 else 1,
            description = "Percentage of targets that received no response. May indicate inattention."
        ))

        // ===== Face metrics =====
        val minuteMultiplier = 60f / durationSeconds

        // Look away count - normalized to per-minute rate
        val lookAwayRate = faceMetrics.lookAwayCount * minuteMultiplier
        val lookAwayFactor = when {
            lookAwayRate > 12 -> 100
            lookAwayRate > 9 -> 80
            lookAwayRate > 6 -> 60
            lookAwayRate > 3 -> 40
            lookAwayRate > 1 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Look Away Rate",
            value = lookAwayRate,
            threshold = 8f,
            significance = if (lookAwayRate > 10) 3 else if (lookAwayRate > 7) 2 else 1,
            description = "How often attention shifts away from the task. Natural to look away occasionally."
        ))

        // Sustained attention score - already on 0-100 scale
        // Note: Here lower scores indicate worse attention (more ADHD-like)
        val invertedAttentionScore = 100 - faceMetrics.sustainedAttentionScore
        val sustainedAttentionFactor = when {
            invertedAttentionScore > 80 -> 100
            invertedAttentionScore > 65 -> 80
            invertedAttentionScore > 50 -> 60
            invertedAttentionScore > 35 -> 40
            invertedAttentionScore > 20 -> 20
            else -> 10
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

        // Look away duration - normalize to 0-100
        val normalizedLookAwayDuration = (faceMetrics.averageLookAwayDuration / 50).coerceIn(0f, 100f)
        val lookAwayDurationFactor = when {
            normalizedLookAwayDuration > 80 -> 100
            normalizedLookAwayDuration > 65 -> 80
            normalizedLookAwayDuration > 50 -> 60
            normalizedLookAwayDuration > 35 -> 40
            normalizedLookAwayDuration > 20 -> 20
            else -> 10
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

        // Attention lapse frequency - normalize to 0-100
        val normalizedAttentionLapses = (faceMetrics.attentionLapseFrequency * 10).coerceIn(0f, 100f)
        val attentionLapseFactor = when {
            normalizedAttentionLapses > 80 -> 100
            normalizedAttentionLapses > 65 -> 80
            normalizedAttentionLapses > 50 -> 60
            normalizedAttentionLapses > 35 -> 40
            normalizedAttentionLapses > 20 -> 20
            else -> 10
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

        // Distractibility index - already on 0-100 scale
        val distractibilityFactor = when {
            faceMetrics.distractibilityIndex > 80 -> 100
            faceMetrics.distractibilityIndex > 65 -> 80
            faceMetrics.distractibilityIndex > 50 -> 60
            faceMetrics.distractibilityIndex > 35 -> 40
            faceMetrics.distractibilityIndex > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Distractibility",
            value = faceMetrics.distractibilityIndex.toFloat(),
            threshold = 70f,
            significance = if (faceMetrics.distractibilityIndex > 80) 3
            else if (faceMetrics.distractibilityIndex > 65) 2
            else 1,
            description = "Overall measure of how easily distracted. Some distractibility is normal."
        ))

        // Blink rate - normalize to 0-100
        val normalizedBlinkRate = (faceMetrics.blinkRate / 0.8f).coerceIn(0f, 100f)
        val blinkRateFactor = when {
            normalizedBlinkRate > 80 -> 60  // High blink rate is moderately associated with ADHD
            normalizedBlinkRate > 65 -> 50
            normalizedBlinkRate > 50 -> 40
            normalizedBlinkRate > 35 -> 30
            normalizedBlinkRate > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Blink Rate",
            value = faceMetrics.blinkRate,
            threshold = 35f,
            significance = if (faceMetrics.blinkRate > 40) 2 else 1,
            description = "Blinks per minute. Excessive blinking can indicate stress or hyperactivity."
        ))

        // Face visibility - already on 0-100 scale
        // For face visibility, lower values mean more problems (scale is reversed)
        val invertedFaceVisibility = 100 - faceMetrics.faceVisiblePercentage
        val faceVisibilityFactor = when {
            invertedFaceVisibility > 40 -> 80  // Very low face visibility is problematic
            invertedFaceVisibility > 30 -> 60
            invertedFaceVisibility > 20 -> 40
            invertedFaceVisibility > 10 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Face Visibility",
            value = faceMetrics.faceVisiblePercentage.toFloat(),
            threshold = 75f,
            significance = if (faceMetrics.faceVisiblePercentage < 60) 2 else 1,
            description = "Percentage of time the face was visible to the camera."
        ))

        // Facial movement - already on 0-100 scale
        val facialMovementFactor = when {
            faceMetrics.facialMovementScore > 80 -> 80
            faceMetrics.facialMovementScore > 65 -> 70
            faceMetrics.facialMovementScore > 50 -> 50
            faceMetrics.facialMovementScore > 35 -> 30
            faceMetrics.facialMovementScore > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Facial Movement",
            value = faceMetrics.facialMovementScore.toFloat(),
            threshold = 65f,
            significance = if (faceMetrics.facialMovementScore > 75) 3
            else if (faceMetrics.facialMovementScore > 60) 2
            else 1,
            description = "Amount of facial movement during task. Some movement is completely normal."
        ))

        // Emotion changes - normalize to 0-100
        val normalizedEmotionChanges = (faceMetrics.emotionChanges * minuteMultiplier).coerceIn(0f, 25f) * 4
        val emotionChangeFactor = when {
            normalizedEmotionChanges > 80 -> 70
            normalizedEmotionChanges > 65 -> 60
            normalizedEmotionChanges > 50 -> 45
            normalizedEmotionChanges > 35 -> 30
            normalizedEmotionChanges > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Emotion Changes",
            value = normalizedEmotionChanges,
            threshold = 50f,
            significance = if (normalizedEmotionChanges > 70) 3 else if (normalizedEmotionChanges > 50) 2 else 1,
            description = "Frequency of emotional expression changes. Rapid changes can indicate impulsivity."
        ))

        // Emotion variability - already on 0-100 scale
        val emotionVariabilityFactor = when {
            faceMetrics.emotionVariabilityScore > 80 -> 70
            faceMetrics.emotionVariabilityScore > 65 -> 60
            faceMetrics.emotionVariabilityScore > 50 -> 45
            faceMetrics.emotionVariabilityScore > 35 -> 30
            faceMetrics.emotionVariabilityScore > 20 -> 20
            else -> 10
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
        // Fidgeting score - already on 0-100 scale
        val fidgetingFactor = when {
            motionMetrics.fidgetingScore > 80 -> 100
            motionMetrics.fidgetingScore > 65 -> 80
            motionMetrics.fidgetingScore > 50 -> 60
            motionMetrics.fidgetingScore > 35 -> 40
            motionMetrics.fidgetingScore > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Fidgeting Score",
            value = motionMetrics.fidgetingScore.toFloat(),
            threshold = 65f,
            significance = if (motionMetrics.fidgetingScore > 75) 3 else if (motionMetrics.fidgetingScore > 60) 2 else 1,
            description = "Small repeated movements. Some fidgeting is normal and not concerning."
        ))

        // Direction changes - normalize to 0-100
        val normalizedDirectionChanges = ((motionMetrics.directionChanges * minuteMultiplier) / 1.5f).coerceIn(0f, 100f)
        val directionChangeFactor = when {
            normalizedDirectionChanges > 80 -> 80
            normalizedDirectionChanges > 65 -> 70
            normalizedDirectionChanges > 50 -> 50
            normalizedDirectionChanges > 35 -> 35
            normalizedDirectionChanges > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Direction Changes",
            value = motionMetrics.directionChanges.toFloat(),
            threshold = 50f,
            significance = if (normalizedDirectionChanges > 75) 3 else if (normalizedDirectionChanges > 50) 2 else 1,
            description = "How often movement direction changes. Rapid shifts can indicate restlessness."
        ))

        // Sudden movements - normalize to 0-100
        val normalizedSuddenMovements = (motionMetrics.suddenMovements * minuteMultiplier).coerceIn(0f, 30f) * 3.33f
        val suddenMovementFactor = when {
            normalizedSuddenMovements > 80 -> 80
            normalizedSuddenMovements > 65 -> 65
            normalizedSuddenMovements > 50 -> 50
            normalizedSuddenMovements > 35 -> 35
            normalizedSuddenMovements > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Sudden Movements",
            value = normalizedSuddenMovements,
            threshold = 60f,
            significance = if (normalizedSuddenMovements > 70) 3 else if (normalizedSuddenMovements > 50) 2 else 1,
            description = "Quick, unexpected movements. Can indicate impulsivity if frequent."
        ))

        // Restlessness - already on 0-100 scale
        val restlessnessFactor = when {
            motionMetrics.restlessness > 80 -> 100
            motionMetrics.restlessness > 65 -> 80
            motionMetrics.restlessness > 50 -> 60
            motionMetrics.restlessness > 35 -> 40
            motionMetrics.restlessness > 20 -> 20
            else -> 10
        }

        markers.add(BehavioralMarker(
            name = "Restlessness",
            value = motionMetrics.restlessness.toFloat(),
            threshold = 65f,
            significance = if (motionMetrics.restlessness > 75) 3 else if (motionMetrics.restlessness > 60) 2 else 1,
            description = "Overall physical activity level. Some movement during tasks is completely normal."
        ))

        // DOMAIN SCORE CALCULATIONS - all factors are now on 0-100 scale for consistency
        // Attention domain
        val attentionScore = ((lookAwayFactor * 0.25f) +
                (sustainedAttentionFactor * 0.25f) +
                (distractibilityFactor * 0.15f) +
                (missedResponseFactor * 0.15f) +
                (accuracyFactor * 0.1f) +
                (responseTimeFactor * 0.1f)).toInt().coerceIn(0, 100)

        // Hyperactivity domain
        val hyperactivityScore = ((fidgetingFactor * 0.30f) +
                (restlessnessFactor * 0.25f) +
                (facialMovementFactor * 0.15f) +
                (directionChangeFactor * 0.15f) +
                (blinkRateFactor * 0.05f) +
                (faceVisibilityFactor * 0.1f)).toInt().coerceIn(0, 100)

        // Impulsivity domain
        val impulsivityScore = ((variabilityFactor * 0.25f) +
                (emotionChangeFactor * 0.2f) +
                (suddenMovementFactor * 0.2f) +
                (emotionVariabilityFactor * 0.15f) +
                (accuracyFactor * 0.1f) +
                (responseTimeFactor * 0.1f)).toInt().coerceIn(0, 100)

        // Overall ADHD probability calculation with weighted domain contributions
        val adhdProbabilityScore = ((attentionScore * 0.45f) +
                (hyperactivityScore * 0.3f) +
                (impulsivityScore * 0.25f)).toInt().coerceIn(0, 100)

        // Calculate confidence level based on data quality
        val confidenceLevel = calculateConfidenceLevel(
            faceMetrics.faceVisiblePercentage,
            durationSeconds,
            markers.size
        )

        return ADHDAssessmentResult(
            adhdProbabilityScore = adhdProbabilityScore,
            attentionScore = attentionScore,
            hyperactivityScore = hyperactivityScore,
            impulsivityScore = impulsivityScore,
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
        var confidence = 75 // Start with higher baseline

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
            faceVisibility > 65 -> 0
            faceVisibility > 50 -> -10
            else -> -20
        }

        // More behavioral markers increases confidence
        confidence += when {
            markerCount >= 15 -> 10
            markerCount >= 10 -> 5
            markerCount >= 8 -> 0
            markerCount >= 6 -> -5
            else -> -10
        }

        return confidence.coerceIn(0, 100)
    }
}