// ADHDAnalyzer.kt
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
    val significance: Int // 1-3 (mild, moderate, high)
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
        val accuracy = if (totalResponses > 0) (correctResponses * 100) / totalResponses else 0

        // Response time factors
        val responseTimeFactor = when {
            averageResponseTime > 600 -> 30
            averageResponseTime > 500 -> 25
            averageResponseTime > 400 -> 15
            averageResponseTime > 300 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Response Time",
            value = averageResponseTime.toFloat(),
            threshold = 400f,
            significance = if (averageResponseTime > 500) 3 else if (averageResponseTime > 400) 2 else 1
        ))

        // Response time variability is a significant marker
        val variabilityFactor = when {
            responseTimeVariability > 200 -> 30
            responseTimeVariability > 150 -> 20
            responseTimeVariability > 100 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Response Variability",
            value = responseTimeVariability,
            threshold = 150f,
            significance = if (responseTimeVariability > 200) 3 else if (responseTimeVariability > 150) 2 else 1
        ))

        // Accuracy factors
        val accuracyFactor = when {
            accuracy < 60 -> 30
            accuracy < 75 -> 20
            accuracy < 85 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Task Accuracy",
            value = accuracy.toFloat(),
            threshold = 80f,
            significance = if (accuracy < 60) 3 else if (accuracy < 75) 2 else 1
        ))

        // Missed responses (particularly important for inattention)
        val missedResponseFactor = when {
            missedResponses > 10 -> 25
            missedResponses > 5 -> 15
            missedResponses > 2 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Missed Responses",
            value = missedResponses.toFloat(),
            threshold = 5f,
            significance = if (missedResponses > 10) 3 else if (missedResponses > 5) 2 else 1
        ))

        // ===== Face metrics =====

        // Calculate normalized rates (per minute)
        val minuteMultiplier = 60f / durationSeconds

        // Look away count (key inattention marker)
        val lookAwayRate = faceMetrics.lookAwayCount * minuteMultiplier
        val lookAwayFactor = when {
            lookAwayRate > 8 -> 35
            lookAwayRate > 5 -> 25
            lookAwayRate > 3 -> 15
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Look Away Rate",
            value = lookAwayRate,
            threshold = 5f,
            significance = if (lookAwayRate > 8) 3 else if (lookAwayRate > 5) 2 else 1
        ))

        // Blink rate (hyperactivity marker)
        val normalizedBlinkRate = faceMetrics.blinkRate
        val blinkRateFactor = when {
            normalizedBlinkRate > 30 -> 15
            normalizedBlinkRate > 25 -> 10
            normalizedBlinkRate > 20 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Blink Rate",
            value = normalizedBlinkRate,
            threshold = 25f,
            significance = if (normalizedBlinkRate > 30) 2 else 1
        ))

        // Face visibility (measure of engagement)
        val faceVisibilityFactor = when {
            faceMetrics.faceVisiblePercentage < 70 -> 20
            faceMetrics.faceVisiblePercentage < 85 -> 10
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Face Visibility",
            value = faceMetrics.faceVisiblePercentage.toFloat(),
            threshold = 85f,
            significance = if (faceMetrics.faceVisiblePercentage < 70) 2 else 1
        ))

        // Emotion changes (impulsivity marker)
        val emotionChangeRate = faceMetrics.emotionChanges * minuteMultiplier
        val emotionChangeFactor = when {
            emotionChangeRate > 5 -> 15
            emotionChangeRate > 3 -> 10
            emotionChangeRate > 2 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Emotion Changes",
            value = emotionChangeRate,
            threshold = 3f,
            significance = if (emotionChangeRate > 5) 2 else 1
        ))

        // ===== Motion metrics =====

        // Fidgeting (hyperactivity marker)
        val fidgetingFactor = when {
            motionMetrics.fidgetingScore > 70 -> 35
            motionMetrics.fidgetingScore > 50 -> 25
            motionMetrics.fidgetingScore > 30 -> 15
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Fidgeting Score",
            value = motionMetrics.fidgetingScore.toFloat(),
            threshold = 50f,
            significance = if (motionMetrics.fidgetingScore > 70) 3 else if (motionMetrics.fidgetingScore > 50) 2 else 1
        ))

        // Direction changes (restlessness marker)
        val directionChangeRate = motionMetrics.directionChanges * minuteMultiplier
        val directionChangeFactor = when {
            directionChangeRate > 50 -> 20
            directionChangeRate > 30 -> 15
            directionChangeRate > 20 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Direction Changes",
            value = directionChangeRate,
            threshold = 30f,
            significance = if (directionChangeRate > 50) 2 else if (directionChangeRate > 30) 2 else 1
        ))

        // Sudden movements (impulsivity marker)
        val suddenMovementRate = motionMetrics.suddenMovements * minuteMultiplier
        val suddenMovementFactor = when {
            suddenMovementRate > 10 -> 20
            suddenMovementRate > 5 -> 10
            suddenMovementRate > 2 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Sudden Movements",
            value = suddenMovementRate,
            threshold = 5f,
            significance = if (suddenMovementRate > 10) 2 else 1
        ))

        // Restlessness (hyperactivity marker)
        val restlessnessFactor = when {
            motionMetrics.restlessness > 70 -> 25
            motionMetrics.restlessness > 50 -> 15
            motionMetrics.restlessness > 30 -> 5
            else -> 0
        }

        markers.add(BehavioralMarker(
            name = "Restlessness",
            value = motionMetrics.restlessness.toFloat(),
            threshold = 50f,
            significance = if (motionMetrics.restlessness > 70) 3 else if (motionMetrics.restlessness > 50) 2 else 1
        ))

        // Calculate scores by domain
        val attentionScore = min(100, lookAwayFactor + missedResponseFactor + faceVisibilityFactor +
                (accuracyFactor / 2) + (responseTimeFactor / 2))

        val hyperactivityScore = min(100, fidgetingFactor + restlessnessFactor + (blinkRateFactor * 2) +
                (directionChangeFactor / 2))

        val impulsivityScore = min(100, emotionChangeFactor + suddenMovementFactor +
                (incorrectResponses * 3) + (variabilityFactor / 2))

        // Calculate overall ADHD probability
        // Weighting: Attention (50%), Hyperactivity (30%), Impulsivity (20%)
        val adhdProbabilityScore = min(100, (attentionScore * 0.5 +
                hyperactivityScore * 0.3 +
                impulsivityScore * 0.2).toInt())

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
            markerCount >= 8 -> 10
            markerCount >= 6 -> 5
            markerCount >= 4 -> 0
            else -> -10
        }

        return confidence.coerceIn(0, 100)
    }
}