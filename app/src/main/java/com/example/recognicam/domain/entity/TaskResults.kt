package com.example.recognicam.domain.entity


import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics

data class CPTTaskResult(
    val correctResponses: Int,
    val incorrectResponses: Int,
    val missedResponses: Int,
    val accuracy: Int,
    val averageResponseTime: Int,
    val fidgetingScore: Int = 0,
    val generalMovementScore: Int = 0,
    val directionChanges: Int = 0,
    val adhdProbabilityScore: Int = 0,
    // Add these fields to support UI functionality
    val responseTimesMs: List<Long> = emptyList(),
    val responseTimeVariability: Float = 0f,
    val faceMetrics: FaceMetrics = FaceMetrics(),
    val motionMetrics: MotionMetrics = MotionMetrics(),
    val adhdAssessment: ADHDAssessmentResult? = null
)
data class ReadingTaskResult(
    val readingTime: Int, // in seconds
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val readingSpeed: Int, // words per minute
    val comprehensionScore: Int,
    val adhdProbabilityScore: Int = 0
)

data class GoNoGoTaskResult(
    val correctGo: Int,
    val correctNoGo: Int,
    val missedGo: Int,
    val incorrectNoGo: Int,
    val averageResponseTime: Int,
    val accuracy: Int,
    val adhdProbabilityScore: Int = 0
)

data class WorkingMemoryTaskResult(
    val correctResponses: Int,
    val incorrectResponses: Int,
    val missedResponses: Int,
    val averageResponseTime: Int,
    val accuracy: Int,
    val memorySpan: Int = 1,
    val adhdProbabilityScore: Int = 0
)

data class AttentionShiftingTaskResult(
    val correctResponses: Int,
    val incorrectResponses: Int,
    val missedResponses: Int,
    val averageResponseTime: Int,
    val accuracy: Int,
    val shiftingCost: Int,
    val adhdProbabilityScore: Int = 0
)