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
        // FIXED: Accuracy calculation that was giving incorrect percentages
        val accuracy = if (totalResponses > 0) {
            (correctResponses * 100) / totalResponses
        } else {
            0
        }

        // Response time factors
        val responseTimeFactor = when {
            averageResponseTime > 700 -> 30
            averageResponseTime > 600 -> 25
            averageResponseTime > 500 -> 15
            averageResponseTime > 400 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Response Time",
            value = averageResponseTime.toFloat(),
            threshold = 500f,
            significance = if (averageResponseTime > 600) 3 else if (averageResponseTime > 500) 2 else 1,
            description = "Average time to respond to target stimuli. Longer times may indicate processing delays."
        ))

        // Response time variability
        val variabilityFactor = when {
            responseTimeVariability > 250 -> 30
            responseTimeVariability > 200 -> 20
            responseTimeVariability > 150 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Response Variability",
            value = responseTimeVariability,
            threshold = 200f,
            significance = if (responseTimeVariability > 250) 3 else if (responseTimeVariability > 200) 2 else 1,
            description = "Consistency of response timing. High variability is a key ADHD indicator."
        ))

        // Accuracy factors
        val accuracyFactor = when {
            accuracy < 50 -> 30
            accuracy < 65 -> 20
            accuracy < 80 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Task Accuracy",
            value = accuracy.toFloat(),
            threshold = 75f,
            significance = if (accuracy < 50) 3 else if (accuracy < 65) 2 else 1,
            description = "Percentage of correct responses. Lower accuracy may indicate attention difficulties."
        ))

        // Missed responses
        val missedResponseFactor = when {
            missedResponses > 15 -> 25
            missedResponses > 8 -> 15
            missedResponses > 4 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Missed Responses",
            value = missedResponses.toFloat(),
            threshold = 8f,
            significance = if (missedResponses > 15) 3 else if (missedResponses > 8) 2 else 1,
            description = "Target stimuli that received no response. May indicate inattention."
        ))

        // ===== Face metrics =====
        val minuteMultiplier = 60f / durationSeconds

        // Look away count
        val lookAwayRate = faceMetrics.lookAwayCount * minuteMultiplier
        val lookAwayFactor = when {
            lookAwayRate > 12 -> 35
            lookAwayRate > 8 -> 25
            lookAwayRate > 5 -> 15
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Look Away Rate",
            value = lookAwayRate,
            threshold = 8f,
            significance = if (lookAwayRate > 12) 3 else if (lookAwayRate > 8) 2 else 1,
            description = "How often attention shifts away from the task. Natural to look away occasionally."
        ))

        // Sustained attention score
        val sustainedAttentionFactor = when {
            faceMetrics.sustainedAttentionScore < 25 -> 25
            faceMetrics.sustainedAttentionScore < 40 -> 15
            faceMetrics.sustainedAttentionScore < 60 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Sustained Attention",
            value = faceMetrics.sustainedAttentionScore.toFloat(),
            threshold = 50f,
            significance = if (faceMetrics.sustainedAttentionScore < 25) 3
            else if (faceMetrics.sustainedAttentionScore < 40) 2
            else 1,
            description = "Ability to maintain focus over time. Lower scores indicate difficulty maintaining attention."
        ))

        // Look away duration
        val lookAwayDurationFactor = when {
            faceMetrics.averageLookAwayDuration > 2500 -> 30
            faceMetrics.averageLookAwayDuration > 2000 -> 20
            faceMetrics.averageLookAwayDuration > 1500 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Look Away Duration",
            value = faceMetrics.averageLookAwayDuration,
            threshold = 2000f,
            significance = if (faceMetrics.averageLookAwayDuration > 2500) 3
            else if (faceMetrics.averageLookAwayDuration > 2000) 2
            else 1,
            description = "How long attention typically stays away from task when distracted."
        ))

        // Attention lapse frequency
        val attentionLapseFactor = when {
            faceMetrics.attentionLapseFrequency > 8 -> 25
            faceMetrics.attentionLapseFrequency > 5 -> 15
            faceMetrics.attentionLapseFrequency > 2 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Attention Lapses",
            value = faceMetrics.attentionLapseFrequency,
            threshold = 5f,
            significance = if (faceMetrics.attentionLapseFrequency > 8) 3
            else if (faceMetrics.attentionLapseFrequency > 5) 2
            else 1,
            description = "Moments when attention completely breaks from the task."
        ))

        // FIXED: Distractibility index - scaled to prevent always showing 100%
        // Apply a scaling factor to normalize the index
        val scaledDistractibility = (faceMetrics.distractibilityIndex * 0.7f).toInt().coerceIn(0, 100)
        val distractibilityFactor = when {
            scaledDistractibility > 85 -> 25
            scaledDistractibility > 70 -> 15
            scaledDistractibility > 50 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Distractibility",
            value = scaledDistractibility.toFloat(),
            threshold = 70f,
            significance = if (scaledDistractibility > 85) 3
            else if (scaledDistractibility > 70) 2
            else 1,
            description = "Overall measure of how easily distracted. Some distractibility is normal."
        ))

        // Blink rate - adjusted to be more realistic
        val normalizedBlinkRate = faceMetrics.blinkRate * 0.8f  // Scale down to avoid over-counting
        val blinkRateFactor = when {
            normalizedBlinkRate > 40 -> 15
            normalizedBlinkRate > 35 -> 10
            normalizedBlinkRate > 30 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Blink Rate",
            value = normalizedBlinkRate,
            threshold = 35f,
            significance = if (normalizedBlinkRate > 40) 2 else 1,
            description = "Blinks per minute. Excessive blinking can indicate stress or hyperactivity."
        ))

        // Face visibility
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

        // FIXED: Facial movement - scale down to avoid overcounting
        val scaledFacialMovement = (faceMetrics.facialMovementScore * 0.7f).toInt().coerceIn(0, 100)
        val facialMovementFactor = when {
            scaledFacialMovement > 80 -> 20
            scaledFacialMovement > 65 -> 10
            scaledFacialMovement > 50 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Facial Movement",
            value = scaledFacialMovement.toFloat(),
            threshold = 65f,
            significance = if (scaledFacialMovement > 80) 3
            else if (scaledFacialMovement > 65) 2
            else 1,
            description = "Amount of facial movement during task. Some movement is completely normal."
        ))

        // FIXED: Emotion changes - scaled to avoid unrealistic counts
        // Normalize to reasonable values
        val scaledEmotionChanges = (faceMetrics.emotionChanges * 0.3f).coerceIn(0f, 30f)
        val emotionChangeRate = scaledEmotionChanges * minuteMultiplier
        val emotionChangeFactor = when {
            emotionChangeRate > 8 -> 15
            emotionChangeRate > 5 -> 10
            emotionChangeRate > 3 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Emotion Changes",
            value = emotionChangeRate,
            threshold = 5f,
            significance = if (emotionChangeRate > 8) 2 else 1,
            description = "Frequency of emotional expression changes. Rapid changes can indicate impulsivity."
        ))

        // Emotion variability
        val emotionVariabilityFactor = when {
            faceMetrics.emotionVariabilityScore > 80 -> 15
            faceMetrics.emotionVariabilityScore > 65 -> 10
            faceMetrics.emotionVariabilityScore > 50 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Emotion Variability",
            value = faceMetrics.emotionVariabilityScore.toFloat(),
            threshold = 65f,
            significance = if (faceMetrics.emotionVariabilityScore > 80) 2
            else if (faceMetrics.emotionVariabilityScore > 65) 2
            else 1,
            description = "Intensity of emotional expression changes. Some variability is normal."
        ))

        // ===== Motion metrics =====
        // FIXED: Better accounting for fidgeting to impact hyperactivity score
        val fidgetingFactor = when {
            motionMetrics.fidgetingScore > 85 -> 35
            motionMetrics.fidgetingScore > 70 -> 25
            motionMetrics.fidgetingScore > 40 -> 15  // Reduced threshold to capture moderate fidgeting
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Fidgeting Score",
            value = motionMetrics.fidgetingScore.toFloat(),
            threshold = 60f,  // Was 70, lowered to be more sensitive
            significance = if (motionMetrics.fidgetingScore > 85) 3 else if (motionMetrics.fidgetingScore > 70) 2 else 1,
            description = "Small repeated movements. Some fidgeting is normal and not concerning."
        ))

        // FIXED: Direction changes - scale down to avoid unrealistically high values
        val scaledDirectionChanges = (motionMetrics.directionChanges * 0.2f).toInt()
        val directionChangeRate = scaledDirectionChanges * minuteMultiplier
        val directionChangeFactor = when {
            directionChangeRate > 70 -> 20
            directionChangeRate > 50 -> 15
            directionChangeRate > 30 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Direction Changes",
            value = directionChangeRate,
            threshold = 50f,
            significance = if (directionChangeRate > 70) 2 else if (directionChangeRate > 50) 2 else 1,
            description = "How often movement direction changes. Rapid shifts can indicate restlessness."
        ))

        // FIXED: Sudden movements - scale down to avoid overcounting
        val scaledSuddenMovements = (motionMetrics.suddenMovements * 0.3f).toInt()
        val suddenMovementRate = scaledSuddenMovements * minuteMultiplier
        val suddenMovementFactor = when {
            suddenMovementRate > 15 -> 20
            suddenMovementRate > 10 -> 10
            suddenMovementRate > 5 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Sudden Movements",
            value = suddenMovementRate,
            threshold = 10f,
            significance = if (suddenMovementRate > 15) 2 else 1,
            description = "Quick, unexpected movements. Can indicate impulsivity if frequent."
        ))

        // Restlessness
        val restlessnessFactor = when {
            motionMetrics.restlessness > 85 -> 25
            motionMetrics.restlessness > 70 -> 15
            motionMetrics.restlessness > 50 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Restlessness",
            value = motionMetrics.restlessness.toFloat(),
            threshold = 70f,
            significance = if (motionMetrics.restlessness > 85) 3 else if (motionMetrics.restlessness > 70) 2 else 1,
            description = "Overall physical activity level. Some movement during tasks is completely normal."
        ))

        // FIXED: Calculate domain scores with better weighting
        // Attention score stays mostly the same but slightly more sensitive
        val attentionScore = min(100, lookAwayFactor + (missedResponseFactor * 2) + (faceVisibilityFactor / 2) +
                sustainedAttentionFactor + (lookAwayDurationFactor / 2) + attentionLapseFactor +
                (accuracyFactor / 3) + (responseTimeFactor / 3))

        // FIXED: Hyperactivity score now better accounts for fidgeting
        val hyperactivityScore = min(100, (fidgetingFactor * 3) + restlessnessFactor + (facialMovementFactor / 2) +
                (blinkRateFactor * 1) + (directionChangeFactor / 2))

        val impulsivityScore = min(100, (emotionChangeFactor / 2) + suddenMovementFactor + (emotionVariabilityFactor / 2) +
                (distractibilityFactor / 2) + (incorrectResponses * 2) + (variabilityFactor / 3))

        // FIXED: Adjusted baseline to be less aggressive
        // Smaller adjustment to allow scores to better reflect actual behaviors
        val baselineAdjustment = 5  // Was 15, reduced significantly to avoid under-reporting

        val adjustedAttentionScore = (attentionScore - baselineAdjustment).coerceIn(0, 100)
        val adjustedHyperactivityScore = (hyperactivityScore - baselineAdjustment).coerceIn(0, 100)
        val adjustedImpulsivityScore = (impulsivityScore - baselineAdjustment).coerceIn(0, 100)

        // FIXED: Overall ADHD probability calculation with adjusted domain weighting
        // Attention still weighs most, but hyperactivity and impulsivity now factor more
        val adhdProbabilityScore = min(100, (adjustedAttentionScore * 0.45 +
                adjustedHyperactivityScore * 0.35 +  // Was 0.25, increased
                adjustedImpulsivityScore * 0.20).toInt())

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