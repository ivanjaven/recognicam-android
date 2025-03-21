package com.example.recognicam.presentation.viewmodel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.base.BaseAssessmentTaskViewModel
import com.example.recognicam.data.analysis.ADHDAnalyzer
import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import com.example.recognicam.domain.entity.AttentionShiftingTaskResult as DomainAttentionShiftingTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.random.Random

enum class Rule {
    COLOR, SHAPE
}

sealed class AttentionShiftingTaskState {
    object Instructions : AttentionShiftingTaskState()
    data class Countdown(val count: Int) : AttentionShiftingTaskState()
    object Running : AttentionShiftingTaskState()
    data class Completed(val result: AttentionShiftingTaskResultUI) : AttentionShiftingTaskState()
}

// UI model for results (matching CPT format)
data class AttentionShiftingTaskResultUI(
    val correctResponses: Int,
    val incorrectResponses: Int,
    val missedResponses: Int,
    val averageResponseTime: Int,
    val responseTimeVariability: Float,
    val accuracy: Int,
    val shiftingCost: Int,
    val ruleShifts: Int,
    val responseTimesMs: List<Long>,
    val faceMetrics: FaceMetrics,
    val motionMetrics: MotionMetrics,
    val adhdAssessment: ADHDAssessmentResult
)

class AttentionShiftingTaskViewModel(
    private val context: Context
) : BaseAssessmentTaskViewModel() {

    private val adhdAnalyzer = ADHDAnalyzer()

    // Task UI state
    private val _uiState = MutableStateFlow<AttentionShiftingTaskState>(AttentionShiftingTaskState.Instructions)
    val uiState: StateFlow<AttentionShiftingTaskState> = _uiState.asStateFlow()

    // Task parameters
    private val _currentShape = MutableStateFlow<String?>(null)
    val currentShape: StateFlow<String?> = _currentShape.asStateFlow()

    private val _currentColor = MutableStateFlow<String?>(null)
    val currentColor: StateFlow<String?> = _currentColor.asStateFlow()

    private val _stimulusVisible = MutableStateFlow(false)
    val stimulusVisible: StateFlow<Boolean> = _stimulusVisible.asStateFlow()

    private val _currentRule = MutableStateFlow<Rule>(Rule.COLOR)
    val currentRule: StateFlow<Rule> = _currentRule.asStateFlow()

    // Performance metrics
    private var correctResponses = 0
    private var incorrectResponses = 0
    private var missedResponses = 0
    private val responseTimes = mutableListOf<Long>()
    private val postShiftResponseTimes = mutableListOf<Long>()
    private val regularResponseTimes = mutableListOf<Long>()

    // Task-specific variables
    private var trialCount = 0
    private var justShifted = false
    private var isTargetStimulus = false
    private var respondedToCurrent = false
    private var ruleShifts = 0
    private var stimulusShownTime = 0L

    // Options for the task
    private val shapes = listOf("square", "circle")
    private val colors = listOf("blue", "red")

    // Stimuli timer
    private var stimulusTimer: CountDownTimer? = null

    init {
        // Just initialize the services without starting them
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.resetTracking()
        }
    }

    fun startCountdown() {
        _uiState.value = AttentionShiftingTaskState.Countdown(3)

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
        postShiftResponseTimes.clear()
        regularResponseTimes.clear()

        // Reset task state
        trialCount = 0
        justShifted = false
        ruleShifts = 0
        _currentRule.value = Rule.COLOR

        // Start sensors
        startSensors()

        // Set up task state
        _uiState.value = AttentionShiftingTaskState.Running

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

        // Check if we missed a target in the previous trial
        if (isTargetStimulus && !respondedToCurrent && trialCount > 0) {
            missedResponses++
        }

        // Wait before showing next stimulus (1-2 seconds)
        val delay = Random.nextInt(1000, 2001).toLong()

        stimulusTimer = object : CountDownTimer(delay, delay) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Increment trial count
                trialCount++

                // Check if we should shift rules - CHANGED FROM EVERY 7 TRIALS TO EVERY 5 TRIALS
                val shouldShiftRule = trialCount % 5 == 0 && trialCount > 0

                if (shouldShiftRule) {
                    _currentRule.value = when (_currentRule.value) {
                        Rule.COLOR -> Rule.SHAPE
                        Rule.SHAPE -> Rule.COLOR
                    }
                    justShifted = true
                    ruleShifts++
                } else {
                    // Reset justShifted after 2 trials following a rule change
                    if (trialCount % 5 >= 2) {
                        justShifted = false
                    }
                }

                // Randomly select shape and color
                val newShape = shapes.random()
                val newColor = colors.random()

                _currentShape.value = newShape
                _currentColor.value = newColor

                // Determine if this is a target based on current rule
                isTargetStimulus = when (_currentRule.value) {
                    Rule.COLOR -> newColor == "blue"
                    Rule.SHAPE -> newShape == "square"
                }

                // Reset response tracking
                respondedToCurrent = false

                // Show the stimulus
                stimulusShownTime = System.currentTimeMillis()
                _stimulusVisible.value = true

                // Set timer for response window (2 seconds)
                stimulusTimer = object : CountDownTimer(2000, 2000) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        // Present next stimulus
                        presentNextStimulus()
                    }
                }.start()
            }
        }.start()
    }

    fun handleResponse() {
        if (_uiState.value != AttentionShiftingTaskState.Running || !_stimulusVisible.value || respondedToCurrent) {
            return
        }

        respondedToCurrent = true

        // Calculate response time
        val responseTime = System.currentTimeMillis() - stimulusShownTime

        if (isTargetStimulus) {
            // Correct response
            correctResponses++

            // Record response time
            responseTimes.add(responseTime)

            // Track if this was just after a rule shift
            if (justShifted) {
                postShiftResponseTimes.add(responseTime)
            } else {
                regularResponseTimes.add(responseTime)
            }
        } else {
            // Incorrect response
            incorrectResponses++
        }

        // Cancel the stimulus timer
        stimulusTimer?.cancel()

        // Present next stimulus
        presentNextStimulus()
    }

    override fun completeTask() {
        // Clean up timers
        mainTimer?.cancel()
        stimulusTimer?.cancel()
        sensorUpdateTimer?.cancel()

        // Final check for missed target
        if (isTargetStimulus && !respondedToCurrent) {
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
        val totalTrials = correctResponses + incorrectResponses + missedResponses
        val accuracy = if (totalTrials > 0) (correctResponses * 100) / totalTrials else 0

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

        // Calculate shifting cost (difference in response time after a rule shift)
        val avgRegularRT = if (regularResponseTimes.isNotEmpty()) {
            regularResponseTimes.average().toInt()
        } else {
            0
        }

        val avgPostShiftRT = if (postShiftResponseTimes.isNotEmpty()) {
            postShiftResponseTimes.average().toInt()
        } else {
            0
        }

        val shiftingCost = if (avgRegularRT > 0 && avgPostShiftRT > 0) {
            kotlin.math.max(0, avgPostShiftRT - avgRegularRT)
        } else {
            0
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
        val domainResult = DomainAttentionShiftingTaskResult(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            shiftingCost = shiftingCost,
            adhdProbabilityScore = adhdAssessment.adhdProbabilityScore
        )

        // Save to repository
        resultsRepository.saveAttentionShiftingResult(domainResult)

        // Create presentation model for UI (matching CPT format)
        val uiResult = AttentionShiftingTaskResultUI(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            averageResponseTime = averageResponseTime,
            responseTimeVariability = responseTimeVariability,
            accuracy = accuracy,
            shiftingCost = shiftingCost,
            ruleShifts = ruleShifts,
            responseTimesMs = responseTimes,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            adhdAssessment = adhdAssessment
        )

        // Update UI state with the presentation model
        _uiState.value = AttentionShiftingTaskState.Completed(uiResult)
    }

    fun processFaceImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (_uiState.value == AttentionShiftingTaskState.Running) {
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
            if (modelClass.isAssignableFrom(AttentionShiftingTaskViewModel::class.java)) {
                return AttentionShiftingTaskViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}