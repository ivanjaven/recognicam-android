package com.example.recognicam.presentation.viewmodel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.base.BaseAssessmentTaskViewModel
import com.example.recognicam.data.analysis.ADHDAnalyzer
import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import com.example.recognicam.domain.entity.WorkingMemoryTaskResult as DomainWorkingMemoryTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.random.Random

sealed class WorkingMemoryTaskState {
    object Instructions : WorkingMemoryTaskState()
    data class Countdown(val count: Int) : WorkingMemoryTaskState()
    object Running : WorkingMemoryTaskState()
    data class Completed(val result: WorkingMemoryTaskResultUI) : WorkingMemoryTaskState()
}

// UI model for results (matching CPT format)
data class WorkingMemoryTaskResultUI(
    val correctResponses: Int,
    val incorrectResponses: Int,
    val missedResponses: Int,
    val averageResponseTime: Int,
    val responseTimeVariability: Float,
    val accuracy: Int,
    val memorySpan: Int = 1,
    val responseTimesMs: List<Long>,
    val faceMetrics: FaceMetrics,
    val motionMetrics: MotionMetrics,
    val adhdAssessment: ADHDAssessmentResult
)

class WorkingMemoryTaskViewModel(
    private val context: Context
) : BaseAssessmentTaskViewModel() {

    private val adhdAnalyzer = ADHDAnalyzer()

    // Task UI state
    private val _uiState = MutableStateFlow<WorkingMemoryTaskState>(WorkingMemoryTaskState.Instructions)
    val uiState: StateFlow<WorkingMemoryTaskState> = _uiState.asStateFlow()

    // Task parameters
    private val _stimulus = MutableStateFlow<String?>(null)
    val stimulus: StateFlow<String?> = _stimulus.asStateFlow()

    private val _stimulusVisible = MutableStateFlow(false)
    val stimulusVisible: StateFlow<Boolean> = _stimulusVisible.asStateFlow()

    // Performance metrics
    private var correctResponses = 0
    private var incorrectResponses = 0
    private var missedResponses = 0
    private val responseTimes = mutableListOf<Long>()

    // Working memory task specific
    private var previousStimulus: String? = null
    private var currentStimulus: String? = null
    private var isCurrentMatch = false
    private var hasResponded = false
    private var trialCount = 0

    // Stimuli for the task (shapes represented as text)
    private val shapes = listOf("●", "■", "▲", "◆", "★", "✚")

    // Timers
    private var stimulusTimer: CountDownTimer? = null
    private var stimulusShownTime = 0L

    init {
        // Just initialize the services without starting them
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.resetTracking()
        }
    }

    fun startCountdown() {
        _uiState.value = WorkingMemoryTaskState.Countdown(3)

        super.startCountdown(3) {
            startTask()
        }
    }

    override fun startTask() {
        // Reset metrics
        correctResponses = 0
        incorrectResponses = 0
        missedResponses = 0
        responseTimes.clear()
        previousStimulus = null
        currentStimulus = null
        trialCount = 0
        isCurrentMatch = false
        hasResponded = false

        // Start sensors
        startSensors()

        // Set up task state
        _uiState.value = WorkingMemoryTaskState.Running

        // Start main timer
        startMainTimer()

        // Start sensor update timer
        startSensorUpdateTimer()

        // Present first stimulus
        presentNextStimulus()
    }

    private fun presentNextStimulus() {
        // Hide current stimulus
        _stimulusVisible.value = false

        // Cancel any existing stimulus timer
        stimulusTimer?.cancel()

        // If we had a stimulus and it was a match but user didn't respond, count as missed
        if (currentStimulus != null && isCurrentMatch && !hasResponded) {
            missedResponses++
        }

        // Wait before showing next stimulus (1-2 seconds)
        val delay = Random.nextInt(1000, 2001).toLong()

        stimulusTimer = object : CountDownTimer(delay, delay) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Update previous stimulus
                previousStimulus = currentStimulus

                // Generate the next stimulus
                if (trialCount == 0 || Random.nextDouble() >= 0.3 || previousStimulus == null) {
                    // Non-match case (first trial, 70% chance, or no previous stimulus)

                    // For first trial, just pick any stimulus
                    if (previousStimulus == null) {
                        currentStimulus = shapes.random()
                    } else {
                        // Pick a different stimulus than the previous one
                        do {
                            currentStimulus = shapes.random()
                        } while (currentStimulus == previousStimulus)
                    }
                    isCurrentMatch = false
                } else {
                    // Match case (30% chance)
                    currentStimulus = previousStimulus
                    isCurrentMatch = true
                }

                // Increment trial count
                trialCount++

                // Update UI
                _stimulus.value = currentStimulus
                hasResponded = false
                stimulusShownTime = System.currentTimeMillis()
                _stimulusVisible.value = true

                // Set timer for stimulus duration (2 seconds)
                stimulusTimer = object : CountDownTimer(2000, 2000) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        // Move to next stimulus
                        presentNextStimulus()
                    }
                }.start()
            }
        }.start()
    }

    fun handleResponse() {
        if (_uiState.value != WorkingMemoryTaskState.Running || !_stimulusVisible.value || hasResponded) {
            return  // Ignore duplicate or invalid responses
        }

        hasResponded = true

        // Calculate response time
        val responseTime = System.currentTimeMillis() - stimulusShownTime

        if (isCurrentMatch) {
            // Correct response to a match
            correctResponses++
            responseTimes.add(responseTime)
        } else {
            // Incorrect response - responded when not a match
            incorrectResponses++
        }

        // Cancel the stimulus timer to move on faster
        stimulusTimer?.cancel()

        // Present next stimulus
        presentNextStimulus()
    }

    override fun completeTask() {
        // Clean up timers
        mainTimer?.cancel()
        stimulusTimer?.cancel()
        sensorUpdateTimer?.cancel()

        // Check for missed response on the final stimulus
        if (currentStimulus != null && isCurrentMatch && !hasResponded) {
            missedResponses++
        }

        // One final update of sensor metrics
        updateSensorMetrics()

        // Get final sensor metrics
        val finalFaceMetrics = _faceMetrics.value
        val finalMotionMetrics = _motionMetrics.value

        // Stop tracking
        if (motionDetectionService.isTracking()) {
            motionDetectionService.stopTracking()
        }

        // Calculate performance metrics
        val totalResponses = correctResponses + incorrectResponses + missedResponses
        val accuracy = if (totalResponses > 0) (correctResponses * 100) / totalResponses else 0

        val averageResponseTime = if (responseTimes.isNotEmpty()) {
            responseTimes.average().toInt()
        } else {
            0
        }

        // Calculate response time variability (standard deviation)
        val responseTimeVariability = if (responseTimes.size > 1) {
            val mean = responseTimes.average()
            val variance = responseTimes.map { (it - mean) * (it - mean) }.sum() / responseTimes.size
            sqrt(variance).toFloat()
        } else {
            0f
        }

        // Analyze ADHD indicators
        val adhdAssessment = adhdAnalyzer.analyzePerformance(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            averageResponseTime = averageResponseTime,
            responseTimeVariability = responseTimeVariability,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            durationSeconds = taskDuration
        )

        // Create domain entity for storage in repository
        val domainResult = DomainWorkingMemoryTaskResult(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            memorySpan = 1, // 1-back task
            adhdProbabilityScore = adhdAssessment.adhdProbabilityScore
        )

        // Save to repository
        resultsRepository.saveWorkingMemoryResult(domainResult)

        // Create presentation model for UI (matching CPT format)
        val uiResult = WorkingMemoryTaskResultUI(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            responseTimeVariability = responseTimeVariability,
            memorySpan = 1, // 1-back task
            responseTimesMs = responseTimes,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            adhdAssessment = adhdAssessment
        )

        // Update UI state with the presentation model
        _uiState.value = WorkingMemoryTaskState.Completed(uiResult)
    }

    fun processFaceImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (_uiState.value == WorkingMemoryTaskState.Running) {
            faceAnalysisService.processImage(imageProxy)
        } else {
            imageProxy.close()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stimulusTimer?.cancel()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkingMemoryTaskViewModel::class.java)) {
                return WorkingMemoryTaskViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}