package com.example.recognicam.presentation.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.domain.entity.AttentionShiftingTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class AttentionShiftingTaskViewModel : ViewModel() {

    private val resultsRepository = ServiceLocator.getResultsRepository()
    private val motionDetectionService = ServiceLocator.getMotionDetectionService()

    // Task UI state
    private val _uiState = MutableStateFlow<AttentionShiftingTaskState>(AttentionShiftingTaskState.Instructions)
    val uiState: StateFlow<AttentionShiftingTaskState> = _uiState.asStateFlow()

    // Task parameters
    private val _timeRemaining = MutableStateFlow(60) // Default 60 seconds
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

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

    // Options for the task
    private val shapes = listOf("square", "circle")
    private val colors = listOf("blue", "red")

    // Task parameters
    private var taskDuration = 60 // seconds

    // Timers
    private var mainTimer: CountDownTimer? = null
    private var stimulusTimer: CountDownTimer? = null
    private var stimulusShownTime = 0L

    init {
        // Start tracking motion when ViewModel is created
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }
    }

    fun startCountdown() {
        _uiState.value = AttentionShiftingTaskState.Countdown(3)

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                _uiState.value = AttentionShiftingTaskState.Countdown(secondsLeft.toInt())
            }

            override fun onFinish() {
                startTask()
            }
        }.start()
    }

    private fun startTask() {
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
        _currentRule.value = Rule.COLOR

        // Reset motion tracking
        motionDetectionService.resetTracking()
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }

        // Set up task state
        _uiState.value = AttentionShiftingTaskState.Running
        _timeRemaining.value = taskDuration

        // Start main timer
        mainTimer = object : CountDownTimer(taskDuration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemaining.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                completeTask()
            }
        }.start()

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

                // Check if we should shift rules (every 7 trials)
                val shouldShiftRule = trialCount % 7 == 0 && trialCount > 0

                if (shouldShiftRule) {
                    _currentRule.value = when (_currentRule.value) {
                        Rule.COLOR -> Rule.SHAPE
                        Rule.SHAPE -> Rule.COLOR
                    }
                    justShifted = true
                } else {
                    // Reset justShifted after 3 trials following a rule change
                    if (trialCount % 7 >= 3) {
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

    private fun completeTask() {
        // Clean up timers
        mainTimer?.cancel()
        stimulusTimer?.cancel()

        // Stop motion tracking and analyze results
        val motionResults = if (motionDetectionService.isTracking()) {
            motionDetectionService.stopTracking()
            motionDetectionService.analyzeMotion()
        } else {
            null
        }

        // Calculate performance metrics
        val totalResponses = correctResponses + incorrectResponses
        val totalTrials = correctResponses + incorrectResponses + missedResponses

        val accuracy = if (totalResponses > 0) (correctResponses * 100) / totalResponses else 0

        val averageResponseTime = if (responseTimes.isNotEmpty()) {
            responseTimes.average().toInt()
        } else {
            0
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
            Math.max(0, avgPostShiftRT - avgRegularRT)
        } else {
            0
        }

        // Calculate ADHD probability
        val shiftingFactor = when {
            shiftingCost > 300 -> 40
            shiftingCost > 200 -> 30
            shiftingCost > 100 -> 20
            else -> 10
        }

        val accuracyFactor = when {
            accuracy < 60 -> 30
            accuracy < 75 -> 20
            accuracy < 85 -> 10
            else -> 0
        }

        val motionFactor = (motionResults?.fidgetingScore?.div(5) ?: 0).coerceAtMost(20)

        val adhdProbabilityScore = (shiftingFactor + accuracyFactor + motionFactor)
            .coerceIn(0, 100)

        val result = AttentionShiftingTaskResult(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            shiftingCost = shiftingCost,
            adhdProbabilityScore = adhdProbabilityScore
        )

        // Save result
        resultsRepository.saveAttentionShiftingResult(result)

        // Update UI state
        _uiState.value = AttentionShiftingTaskState.Completed(result)
    }

    fun setTaskDuration(seconds: Int) {
        if (_uiState.value == AttentionShiftingTaskState.Instructions) {
            taskDuration = seconds
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainTimer?.cancel()
        stimulusTimer?.cancel()
    }
}

enum class Rule {
    COLOR, SHAPE
}

sealed class AttentionShiftingTaskState {
    object Instructions : AttentionShiftingTaskState()
    data class Countdown(val count: Int) : AttentionShiftingTaskState()
    object Running : AttentionShiftingTaskState()
    data class Completed(val result: AttentionShiftingTaskResult) : AttentionShiftingTaskState()
}