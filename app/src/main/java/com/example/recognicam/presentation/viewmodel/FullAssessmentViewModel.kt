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
    val overallScore: Int,                     // Overall ADHD probability (0-100)
    val confidenceLevel: Int,                  // Confidence in assessment (0-100)
    val attentionScore: Int,                   // Inattention domain score (0-100)
    val hyperactivityScore: Int,               // Hyperactivity domain score (0-100)
    val impulsivityScore: Int,                 // Impulsivity domain score (0-100)
    val taskResults: Map<String, Any>,         // Individual task results
    val faceMetrics: FaceMetrics,              // Face analysis metrics
    val motionMetrics: MotionMetrics,          // Motion analysis metrics
    val behavioralMarkers: List<BehavioralMarker>, // Combined behavioral markers
    val assessmentDuration: Long               // Total duration in milliseconds
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

        // Calculate comprehensive domain scores based on all tasks
        val attentionScores = calculateAttentionScores()
        val hyperactivityScores = calculateHyperactivityScores()
        val impulsivityScores = calculateImpulsivityScores()

        // Combine behavioral markers from all tasks
        val allMarkers = combineMarkers()

        // Get the latest sensor metrics from the most recent task (usually attention shifting)
        val latestFaceMetrics = getLatestFaceMetrics()
        val latestMotionMetrics = getLatestMotionMetrics()

        // Calculate overall ADHD probability with proper domain weighting
        val overallScore = (attentionScores.average() * 0.45 +
                hyperactivityScores.average() * 0.3 +
                impulsivityScores.average() * 0.25).toInt()

        // Calculate confidence level
        val confidenceLevel = calculateConfidenceLevel(
            faceVisibility = latestFaceMetrics.faceVisiblePercentage,
            tasksCompleted = countCompletedTasks(),
            duration = (totalDuration / 1000 / 60).toInt(), // in minutes
            markerCount = allMarkers.size
        )

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
            overallScore = overallScore,
            confidenceLevel = confidenceLevel,
            attentionScore = attentionScores.average().toInt(),
            hyperactivityScore = hyperactivityScores.average().toInt(),
            impulsivityScore = impulsivityScores.average().toInt(),
            taskResults = taskResults,
            faceMetrics = latestFaceMetrics,
            motionMetrics = latestMotionMetrics,
            behavioralMarkers = allMarkers,
            assessmentDuration = totalDuration
        )

        // Update UI state with the result
        _uiState.value = FullAssessmentState.Completed(fullResult)
    }

    private fun calculateAttentionScores(): List<Int> {
        val scores = mutableListOf<Int>()

        // Add CPT attention score with higher weight (primary attention task)
        cptResult?.adhdAssessment?.attentionScore?.let {
            scores.add((it * 1.2).toInt().coerceIn(0, 100))
        }

        // Add Reading task attention score
        readingResult?.adhdAssessment?.attentionScore?.let {
            scores.add(it)
        }

        // Add Go/No-Go attention component
        goNoGoResult?.adhdAssessment?.attentionScore?.let {
            scores.add(it)
        }

        // Add Working Memory attention component
        workingMemoryResult?.adhdAssessment?.attentionScore?.let {
            scores.add(it)
        }

        // Add Attention Shifting attention component with higher weight
        attentionShiftingResult?.adhdAssessment?.attentionScore?.let {
            scores.add((it * 1.1).toInt().coerceIn(0, 100))
        }

        return scores.ifEmpty { listOf(0) }
    }

    private fun calculateHyperactivityScores(): List<Int> {
        val scores = mutableListOf<Int>()

        // Add hyperactivity scores from each task
        cptResult?.adhdAssessment?.hyperactivityScore?.let {
            scores.add(it)
        }

        readingResult?.adhdAssessment?.hyperactivityScore?.let {
            scores.add(it)
        }

        // Go/No-Go has more hyperactivity relevance
        goNoGoResult?.adhdAssessment?.hyperactivityScore?.let {
            scores.add((it * 1.1).toInt().coerceIn(0, 100))
        }

        workingMemoryResult?.adhdAssessment?.hyperactivityScore?.let {
            scores.add(it)
        }

        attentionShiftingResult?.adhdAssessment?.hyperactivityScore?.let {
            scores.add(it)
        }

        return scores.ifEmpty { listOf(0) }
    }

    private fun calculateImpulsivityScores(): List<Int> {
        val scores = mutableListOf<Int>()

        // Add impulsivity scores from each task
        cptResult?.adhdAssessment?.impulsivityScore?.let {
            scores.add(it)
        }

        readingResult?.adhdAssessment?.impulsivityScore?.let {
            scores.add(it)
        }

        // Go/No-Go has more impulsivity relevance (highest weight)
        goNoGoResult?.adhdAssessment?.impulsivityScore?.let {
            scores.add((it * 1.3).toInt().coerceIn(0, 100))
        }

        workingMemoryResult?.adhdAssessment?.impulsivityScore?.let {
            scores.add(it)
        }

        attentionShiftingResult?.adhdAssessment?.impulsivityScore?.let {
            scores.add(it)
        }

        return scores.ifEmpty { listOf(0) }
    }

    private fun combineMarkers(): List<BehavioralMarker> {
        val allMarkers = mutableListOf<BehavioralMarker>()

        // Collect markers from all tasks
        cptResult?.adhdAssessment?.behavioralMarkers?.let {
            allMarkers.addAll(it)
        }

        readingResult?.adhdAssessment?.behavioralMarkers?.let {
            allMarkers.addAll(it)
        }

        goNoGoResult?.adhdAssessment?.behavioralMarkers?.let {
            allMarkers.addAll(it)
        }

        workingMemoryResult?.adhdAssessment?.behavioralMarkers?.let {
            allMarkers.addAll(it)
        }

        attentionShiftingResult?.adhdAssessment?.behavioralMarkers?.let {
            allMarkers.addAll(it)
        }

        // Group by marker name and take the most significant for each
        return allMarkers
            .groupBy { it.name }
            .map { (_, markers) ->
                // For each marker name, pick the one with highest significance * (value/threshold) ratio
                markers.maxByOrNull {
                    it.significance * (it.value / it.threshold)
                } ?: markers.first()
            }
            .sortedByDescending {
                it.significance * (it.value / it.threshold)
            }
    }

    private fun getLatestFaceMetrics(): FaceMetrics {
        return attentionShiftingResult?.faceMetrics ?:
        workingMemoryResult?.faceMetrics ?:
        goNoGoResult?.faceMetrics ?:
        readingResult?.faceMetrics ?:
        cptResult?.faceMetrics ?:
        FaceMetrics()
    }

    private fun getLatestMotionMetrics(): MotionMetrics {
        return attentionShiftingResult?.motionMetrics ?:
        workingMemoryResult?.motionMetrics ?:
        goNoGoResult?.motionMetrics ?:
        readingResult?.motionMetrics ?:
        cptResult?.motionMetrics ?:
        MotionMetrics()
    }

    private fun countCompletedTasks(): Int {
        var count = 0
        if (cptResult != null) count++
        if (readingResult != null) count++
        if (goNoGoResult != null) count++
        if (workingMemoryResult != null) count++
        if (attentionShiftingResult != null) count++
        return count
    }

    private fun calculateConfidenceLevel(
        faceVisibility: Int,
        tasksCompleted: Int,
        duration: Int,
        markerCount: Int
    ): Int {
        // Start with a high baseline confidence
        var confidence = 80

        // Number of completed tasks has high impact
        confidence += when (tasksCompleted) {
            5 -> 20
            4 -> 15
            3 -> 5
            2 -> -5
            1 -> -15
            else -> -20
        }

        // Face visibility affects confidence
        confidence += when {
            faceVisibility > 90 -> 10
            faceVisibility > 80 -> 5
            faceVisibility > 70 -> 0
            faceVisibility > 60 -> -5
            else -> -10
        }

        // Total assessment duration
        confidence += when {
            duration >= 15 -> 10
            duration >= 10 -> 5
            duration >= 5 -> 0
            else -> -5
        }

        // Marker count provides more data points
        confidence += when {
            markerCount >= 15 -> 5
            markerCount >= 10 -> 3
            markerCount >= 5 -> 0
            else -> -5
        }

        return confidence.coerceIn(0, 100)
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