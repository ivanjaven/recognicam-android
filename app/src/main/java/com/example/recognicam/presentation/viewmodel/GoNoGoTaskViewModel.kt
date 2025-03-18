package com.example.recognicam.presentation.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.domain.entity.GoNoGoTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class GoNoGoTaskViewModel : ViewModel() {

    private val resultsRepository = ServiceLocator.getResultsRepository()
    private val motionDetectionService = ServiceLocator.getMotionDetectionService()

    // Task UI state
    private val _uiState = MutableStateFlow<GoNoGoTaskState>(GoNoGoTaskState.Instructions)
    val uiState: StateFlow<GoNoGoTaskState> = _uiState.asStateFlow()

    // Task parameters
    private val _timeRemaining = MutableStateFlow(40) // Default 40 seconds
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _stimulusType = MutableStateFlow<StimulusType?>(null)
    val stimulusType: StateFlow<StimulusType?> = _stimulusType.asStateFlow()

    private val _stimulusVisible = MutableStateFlow(false)
    val stimulusVisible: StateFlow<Boolean> = _stimulusVisible.asStateFlow()

    // Performance metrics
    private var correctGo = 0
    private var correctNoGo = 0
    private var missedGo = 0
    private var incorrectNoGo = 0
    private val goResponseTimes = mutableListOf<Long>()

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
        _uiState.value = GoNoGoTaskState.Countdown(3)

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                _uiState.value = GoNoGoTaskState.Countdown(secondsLeft.toInt())
            }

            override fun onFinish() {
                startTask()
            }
        }.start()
    }

    private fun startTask() {
        // Reset metrics
        correctGo = 0
        correctNoGo = 0
        missedGo = 0
        incorrectNoGo = 0
        goResponseTimes.clear()

        // Reset motion tracking
        motionDetectionService.resetTracking()
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }

        // Set up task state
        _uiState.value = GoNoGoTaskState.Running
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
        _stimulusType.value = null

        // Cancel any existing stimulus timer
        stimulusTimer?.cancel()

        // Random delay between stimuli (1000-2500ms)
        val delay = Random.nextInt(1000, 2501).toLong()

        stimulusTimer = object : CountDownTimer(delay, delay) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Determine stimulus type (70% Go, 30% No-Go)
                val isGo = Random.nextDouble() < 0.7
                _stimulusType.value = if (isGo) StimulusType.GO else StimulusType.NO_GO

                // Show the stimulus
                stimulusShownTime = System.currentTimeMillis()
                _stimulusVisible.value = true

                // Set timer for missed response (1.5 seconds)
                stimulusTimer = object : CountDownTimer(1500, 1500) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        // Process response timeout
                        if (_stimulusVisible.value) {
                            if (_stimulusType.value == StimulusType.GO) {
                                // Missed Go stimulus
                                missedGo++
                            } else {
                                // Correctly ignored No-Go stimulus
                                correctNoGo++
                            }

                            // Hide the stimulus
                            _stimulusVisible.value = false
                        }

                        // Present next stimulus
                        presentNextStimulus()
                    }
                }.start()
            }
        }.start()
    }

    fun handleResponse() {
        if (_uiState.value != GoNoGoTaskState.Running || !_stimulusVisible.value) {
            return
        }

        // Cancel the stimulus timer
        stimulusTimer?.cancel()

        when (_stimulusType.value) {
            StimulusType.GO -> {
                // Correct Go response
                correctGo++

                // Record response time
                val responseTime = System.currentTimeMillis() - stimulusShownTime
                goResponseTimes.add(responseTime)
            }
            StimulusType.NO_GO -> {
                // Incorrect No-Go response
                incorrectNoGo++
            }
            else -> {
                // No stimulus or unknown type - should not happen
                return
            }
        }

        // Hide the stimulus
        _stimulusVisible.value = false

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
        val totalTrials = correctGo + correctNoGo + missedGo + incorrectNoGo
        val accuracy = if (totalTrials > 0) {
            ((correctGo + correctNoGo) * 100) / totalTrials
        } else {
            0
        }

        val averageResponseTime = if (goResponseTimes.isNotEmpty()) {
            goResponseTimes.average().toInt()
        } else {
            0
        }

        // Calculate ADHD probability score
        val responseTimeFactor = if (averageResponseTime > 500) 30 else
            if (averageResponseTime > 400) 20 else
                if (averageResponseTime > 300) 10 else 0

        val inhibitionErrorFactor = (incorrectNoGo * 5).coerceAtMost(40)
        val missedGoFactor = (missedGo * 3).coerceAtMost(30)
        val motionFactor = (motionResults?.fidgetingScore?.div(5) ?: 0).coerceAtMost(20)

        val adhdProbabilityScore = (responseTimeFactor + inhibitionErrorFactor +
                missedGoFactor + motionFactor)
            .coerceIn(0, 100)

        val result = GoNoGoTaskResult(
            correctGo = correctGo,
            correctNoGo = correctNoGo,
            missedGo = missedGo,
            incorrectNoGo = incorrectNoGo,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            adhdProbabilityScore = adhdProbabilityScore
        )

        // Save result
        resultsRepository.saveGoNoGoResult(result)

        // Update UI state
        _uiState.value = GoNoGoTaskState.Completed(result)
    }

    fun setTaskDuration(seconds: Int) {
        if (_uiState.value == GoNoGoTaskState.Instructions) {
            taskDuration = seconds
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainTimer?.cancel()
        stimulusTimer?.cancel()
    }
}

enum class StimulusType {
    GO, NO_GO
}

sealed class GoNoGoTaskState {
    object Instructions : GoNoGoTaskState()
    data class Countdown(val count: Int) : GoNoGoTaskState()
    object Running : GoNoGoTaskState()
    data class Completed(val result: GoNoGoTaskResult) : GoNoGoTaskState()
}