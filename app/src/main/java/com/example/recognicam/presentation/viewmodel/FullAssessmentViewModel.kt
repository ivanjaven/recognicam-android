// FullAssessmentViewModel.kt
package com.example.recognicam.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.data.analysis.ADHDAnalyzer
import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.analysis.BehavioralMarker
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class FullAssessmentState {
    object Instructions : FullAssessmentState()
    object StartCPTTask : FullAssessmentState()
    object StartReadingTask : FullAssessmentState()
    object StartGoNoGoTask : FullAssessmentState()
    object StartWorkingMemoryTask : FullAssessmentState()
    object StartAttentionShiftingTask : FullAssessmentState()
    data class TaskTransition(val from: String, val to: String, val message: String) : FullAssessmentState()
    data class Completed(val result: FullAssessmentResult) : FullAssessmentState()
}

data class FullAssessmentResult(
    val overallScore: Int,
    val confidenceLevel: Int,
    val attentionScore: Int,
    val hyperactivityScore: Int,
    val impulsivityScore: Int,
    val taskResults: Map<String, Any>,
    val faceMetrics: FaceMetrics,
    val motionMetrics: MotionMetrics,
    val behavioralMarkers: List<BehavioralMarker>,
    val assessmentDuration: Long
)

class FullAssessmentViewModel(private val context: Context) : ViewModel() {

    private val resultsRepository = ServiceLocator.getResultsRepository()
    private val adhdAnalyzer = ADHDAnalyzer()

    private val _uiState = MutableStateFlow<FullAssessmentState>(FullAssessmentState.Instructions)
    val uiState: StateFlow<FullAssessmentState> = _uiState.asStateFlow()

    // Task results storage
    private var cptResult: CPTTaskResult? = null
    private var readingResult: ReadingTaskResultUI? = null
    private var goNoGoResult: GoNoGoTaskResultUI? = null
    private var workingMemoryResult: WorkingMemoryTaskResultUI? = null
    private var attentionShiftingResult: AttentionShiftingTaskResultUI? = null

    // Assessment timing
    private var startTime = 0L

    fun startAssessment() {
        startTime = System.currentTimeMillis()

        // Clear any previous results
        resultsRepository.clearAllResults()

        // Move to the first task
        _uiState.value = FullAssessmentState.StartCPTTask
    }

    fun saveCPTResult(result: CPTTaskResult) {
        cptResult = result
        // Show a transition screen
        _uiState.value = FullAssessmentState.TaskTransition(
            from = "Continuous Performance Test",
            to = "Reading Assessment",
            message = "Great job! Let's move on to the next task."
        )
    }

    fun proceedToReadingTask() {
        _uiState.value = FullAssessmentState.StartReadingTask
    }

    fun saveReadingResult(result: ReadingTaskResultUI) {
        readingResult = result
        // Show a transition screen
        _uiState.value = FullAssessmentState.TaskTransition(
            from = "Reading Assessment",
            to = "Go/No-Go Task",
            message = "Good work! Moving to the inhibition control task."
        )
    }

    fun proceedToGoNoGoTask() {
        _uiState.value = FullAssessmentState.StartGoNoGoTask
    }

    fun saveGoNoGoResult(result: GoNoGoTaskResultUI) {
        goNoGoResult = result
        // Show a transition screen
        _uiState.value = FullAssessmentState.TaskTransition(
            from = "Go/No-Go Task",
            to = "Working Memory Task",
            message = "Well done! Next is the working memory task."
        )
    }

    fun proceedToWorkingMemoryTask() {
        _uiState.value = FullAssessmentState.StartWorkingMemoryTask
    }

    fun saveWorkingMemoryResult(result: WorkingMemoryTaskResultUI) {
        workingMemoryResult = result
        // Show a transition screen
        _uiState.value = FullAssessmentState.TaskTransition(
            from = "Working Memory Task",
            to = "Attention Shifting Task",
            message = "Almost done! Final task: attention shifting."
        )
    }

    fun proceedToAttentionShiftingTask() {
        _uiState.value = FullAssessmentState.StartAttentionShiftingTask
    }

    fun saveAttentionShiftingResult(result: AttentionShiftingTaskResultUI) {
        attentionShiftingResult = result
        // Show a transition screen
        _uiState.value = FullAssessmentState.TaskTransition(
            from = "Attention Shifting Task",
            to = "Results",
            message = "All tasks completed! Compiling your results..."
        )

        // Start processing the results
        compileResults()
    }

    private fun compileResults() {
        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - startTime

        // Calculate overall ADHD probability score (weighted average)
        val scores = listOfNotNull(
            cptResult?.adhdAssessment?.adhdProbabilityScore,
            readingResult?.adhdAssessment?.adhdProbabilityScore,
            goNoGoResult?.adhdAssessment?.adhdProbabilityScore,
            workingMemoryResult?.adhdAssessment?.adhdProbabilityScore,
            attentionShiftingResult?.adhdAssessment?.adhdProbabilityScore
        )

        // If no scores, we can't proceed
        if (scores.isEmpty()) {
            // This shouldn't happen normally since we guide through all tasks
            return
        }

        // Calculate composite scores using proper collection methods
        val attentionScores = mutableListOf<Float>()
        cptResult?.adhdAssessment?.attentionScore?.let { attentionScores.add(it * 1.2f) }
        readingResult?.adhdAssessment?.attentionScore?.let { attentionScores.add(it.toFloat()) }
        goNoGoResult?.adhdAssessment?.attentionScore?.let { attentionScores.add(it.toFloat()) }
        workingMemoryResult?.adhdAssessment?.attentionScore?.let { attentionScores.add(it.toFloat()) }
        attentionShiftingResult?.adhdAssessment?.attentionScore?.let { attentionScores.add(it * 1.1f) }

        val hyperactivityScores = mutableListOf<Float>()
        cptResult?.adhdAssessment?.hyperactivityScore?.let { hyperactivityScores.add(it.toFloat()) }
        readingResult?.adhdAssessment?.hyperactivityScore?.let { hyperactivityScores.add(it.toFloat()) }
        goNoGoResult?.adhdAssessment?.hyperactivityScore?.let { hyperactivityScores.add(it * 1.1f) }
        workingMemoryResult?.adhdAssessment?.hyperactivityScore?.let { hyperactivityScores.add(it.toFloat()) }
        attentionShiftingResult?.adhdAssessment?.hyperactivityScore?.let { hyperactivityScores.add(it.toFloat()) }

        val impulsivityScores = mutableListOf<Float>()
        cptResult?.adhdAssessment?.impulsivityScore?.let { impulsivityScores.add(it.toFloat()) }
        readingResult?.adhdAssessment?.impulsivityScore?.let { impulsivityScores.add(it.toFloat()) }
        goNoGoResult?.adhdAssessment?.impulsivityScore?.let { impulsivityScores.add(it * 1.3f) }
        workingMemoryResult?.adhdAssessment?.impulsivityScore?.let { impulsivityScores.add(it.toFloat()) }
        attentionShiftingResult?.adhdAssessment?.impulsivityScore?.let { impulsivityScores.add(it.toFloat()) }

        // Calculate composite domain scores
        val compositeAttentionScore = if (attentionScores.isNotEmpty()) {
            attentionScores.sum() / attentionScores.size
        } else 0f

        val compositeHyperactivityScore = if (hyperactivityScores.isNotEmpty()) {
            hyperactivityScores.sum() / hyperactivityScores.size
        } else 0f

        val compositeImpulsivityScore = if (impulsivityScores.isNotEmpty()) {
            impulsivityScores.sum() / impulsivityScores.size
        } else 0f

        // Calculate overall probability (40% attention, 30% hyperactivity, 30% impulsivity)
        val overallProbability = (compositeAttentionScore.toInt() * 0.4 +
                compositeHyperactivityScore.toInt() * 0.3 +
                compositeImpulsivityScore.toInt() * 0.3).toInt()

        // Determine the highest confidence level from individual assessments
        val confidenceLevels = listOfNotNull(
            cptResult?.adhdAssessment?.confidenceLevel,
            readingResult?.adhdAssessment?.confidenceLevel,
            goNoGoResult?.adhdAssessment?.confidenceLevel,
            workingMemoryResult?.adhdAssessment?.confidenceLevel,
            attentionShiftingResult?.adhdAssessment?.confidenceLevel
        )

        val overallConfidence = if (confidenceLevels.isNotEmpty()) {
            // Slight boost to confidence since we have multiple assessments
            (confidenceLevels.average() * 1.1).toInt().coerceAtMost(100)
        } else {
            70 // Default reasonable confidence
        }

        // Combine behavioral markers (taking the most significant ones)
        val allMarkers = mutableListOf<BehavioralMarker>()
        cptResult?.adhdAssessment?.behavioralMarkers?.let { allMarkers.addAll(it) }
        readingResult?.adhdAssessment?.behavioralMarkers?.let { allMarkers.addAll(it) }
        goNoGoResult?.adhdAssessment?.behavioralMarkers?.let { allMarkers.addAll(it) }
        workingMemoryResult?.adhdAssessment?.behavioralMarkers?.let { allMarkers.addAll(it) }
        attentionShiftingResult?.adhdAssessment?.behavioralMarkers?.let { allMarkers.addAll(it) }

        // Get the most significant markers (avoid duplicates by name)
        val significantMarkers = allMarkers
            .groupBy { it.name }
            .map { (_, markers) ->
                // For each marker name, pick the one with highest significance * (value/threshold)
                markers.maxByOrNull { it.significance * (it.value / it.threshold) }
            }
            .filterNotNull()
            .sortedByDescending { it.significance * (it.value / it.threshold) }
            .take(12) // Take top 12 most significant markers

        // Get the latest sensor metrics from the last task (should be attention shifting)
        val latestFaceMetrics = attentionShiftingResult?.faceMetrics ?:
        workingMemoryResult?.faceMetrics ?:
        goNoGoResult?.faceMetrics ?:
        readingResult?.faceMetrics ?:
        cptResult?.faceMetrics ?:
        FaceMetrics()

        val latestMotionMetrics = attentionShiftingResult?.motionMetrics ?:
        workingMemoryResult?.motionMetrics ?:
        goNoGoResult?.motionMetrics ?:
        readingResult?.motionMetrics ?:
        cptResult?.motionMetrics ?:
        MotionMetrics()

        // Create a map of task results
        val taskResults = mapOf(
            "CPT" to (cptResult ?: "Not completed"),
            "Reading" to (readingResult ?: "Not completed"),
            "GoNoGo" to (goNoGoResult ?: "Not completed"),
            "WorkingMemory" to (workingMemoryResult ?: "Not completed"),
            "AttentionShifting" to (attentionShiftingResult ?: "Not completed")
        )

        // Create the full assessment result
        val fullResult = FullAssessmentResult(
            overallScore = overallProbability,
            confidenceLevel = overallConfidence,
            attentionScore = compositeAttentionScore.toInt(),
            hyperactivityScore = compositeHyperactivityScore.toInt(),
            impulsivityScore = compositeImpulsivityScore.toInt(),
            taskResults = taskResults,
            faceMetrics = latestFaceMetrics,
            motionMetrics = latestMotionMetrics,
            behavioralMarkers = significantMarkers,
            assessmentDuration = totalDuration
        )

        // Update UI state with the result
        _uiState.value = FullAssessmentState.Completed(fullResult)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FullAssessmentViewModel::class.java)) {
                return FullAssessmentViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}