package com.example.recognicam.presentation.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.domain.entity.WorkingMemoryTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class WorkingMemoryTaskViewModel : ViewModel() {

    private val resultsRepository = ServiceLocator.getResultsRepository()
    private val motionDetectionService = ServiceLocator.getMotionDetectionService()

    // Task UI state
    private val _uiState = MutableStateFlow<WorkingMemoryTaskState>(WorkingMemoryTaskState.Instructions)
    val uiState: StateFlow<WorkingMemoryTaskState> = _uiState.asStateFlow()

    // Task parameters
    private val _timeRemaining = MutableStateFlow(40) // Default 40 seconds
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

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

    // Stimuli for the task (shapes represented as text)
    private val shapes = listOf("●", "■", "▲", "◆", "★", "✚")

    // Task parameters
    private var taskDuration = 40 // seconds

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
        _uiState.value = WorkingMemoryTaskState.Countdown(3)

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                _uiState.value = WorkingMemoryTaskState.Countdown(secondsLeft.toInt())
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
        previousStimulus = null
        currentStimulus = null

        // Reset motion tracking
        motionDetectionService.resetTracking()
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }

        // Set up task state
        _uiState.value = WorkingMemoryTaskState.Running
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

        // Wait before showing next stimulus (1-2 seconds)
        val delay = Random.nextInt(1000, 2001).toLong()

        stimulusTimer = object : CountDownTimer(delay, delay) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Update stimuli
                previousStimulus = currentStimulus

                // Determine if this should be a match (20% chance if not first stimulus)
                val shouldBeMatch = previousStimulus != null && Random.nextDouble() < 0.2

                if (shouldBeMatch) {
                    // Match - use the same shape as previous
                    currentStimulus = previousStimulus
                    isCurrentMatch = true
                } else {
                    // Find a different shape than the previous
                    var newStimulus: String
                    do {
                        newStimulus = shapes.random()
                    } while (newStimulus == previousStimulus)

                    currentStimulus = newStimulus
                    isCurrentMatch = false
                }

                // Update UI
                _stimulus.value = currentStimulus

                // Reset response tracking
                hasResponded = false

                // Show the stimulus
                stimulusShownTime = System.currentTimeMillis()
                _stimulusVisible.value = true

                // Set timer for missed response (2 seconds)
                stimulusTimer = object : CountDownTimer(2000, 2000) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        // If it was a match and no response, count as missed
                        if (isCurrentMatch && !hasResponded) {
                            missedResponses++
                        }

                        // Show next stimulus
                        presentNextStimulus()
                    }
                }.start()
            }
        }.start()
    }

    fun handleResponse() {
        if (_uiState.value != WorkingMemoryTaskState.Running || !_stimulusVisible.value || hasResponded) {
            return
        }

        hasResponded = true

        // Calculate response time
        val responseTime = System.currentTimeMillis() - stimulusShownTime

        if (isCurrentMatch) {
            // Correct response to a match
            correctResponses++
            responseTimes.add(responseTime)
        } else {
            // Incorrect response - not a match
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
        val totalResponses = correctResponses + incorrectResponses + missedResponses
        val accuracy = if (totalResponses > 0) (correctResponses * 100) / totalResponses else 0

        val averageResponseTime = if (responseTimes.isNotEmpty()) {
            responseTimes.average().toInt()
        } else {
            0
        }

        // Calculate ADHD probability score
        val accuracyFactor = when {
            accuracy < 60 -> 40
            accuracy < 75 -> 30
            accuracy < 85 -> 20
            else -> 10
        }

        val responseTimeFactor = when {
            averageResponseTime > 600 -> 30
            averageResponseTime > 500 -> 20
            averageResponseTime > 400 -> 10
            else -> 0
        }

        val motionFactor = (motionResults?.fidgetingScore?.div(4) ?: 0).coerceAtMost(30)

        val adhdProbabilityScore = (accuracyFactor + responseTimeFactor + motionFactor)
            .coerceIn(0, 100)

        val result = WorkingMemoryTaskResult(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            memorySpan = 1, // 1-back task
            adhdProbabilityScore = adhdProbabilityScore
        )

        // Save result
        resultsRepository.saveWorkingMemoryResult(result)

        // Update UI state
        _uiState.value = WorkingMemoryTaskState.Completed(result)
    }

    fun setTaskDuration(seconds: Int) {
        if (_uiState.value == WorkingMemoryTaskState.Instructions) {
            taskDuration = seconds
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainTimer?.cancel()
        stimulusTimer?.cancel()
    }
}

sealed class WorkingMemoryTaskState {
    object Instructions : WorkingMemoryTaskState()
    data class Countdown(val count: Int) : WorkingMemoryTaskState()
    object Running : WorkingMemoryTaskState()
    data class Completed(val result: WorkingMemoryTaskResult) : WorkingMemoryTaskState()
}