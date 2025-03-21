package com.example.recognicam.presentation.viewmodel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.base.BaseAssessmentTaskViewModel
import com.example.recognicam.data.analysis.ADHDAnalyzer
import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import com.example.recognicam.domain.entity.GoNoGoTaskResult as DomainGoNoGoTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.random.Random

sealed class GoNoGoTaskState {
    object Instructions : GoNoGoTaskState()
    data class Countdown(val count: Int) : GoNoGoTaskState()
    object Running : GoNoGoTaskState()
    data class Completed(val result: GoNoGoTaskResultUI) : GoNoGoTaskState()
}

enum class StimulusType {
    GO, NO_GO
}

// UI model for results (matching CPT format)
data class GoNoGoTaskResultUI(
    val correctGo: Int,
    val correctNoGo: Int,
    val missedGo: Int,
    val incorrectNoGo: Int,
    val averageResponseTime: Int,
    val accuracy: Int,
    val responseTimesMs: List<Long>,
    val responseTimeVariability: Float,
    val faceMetrics: FaceMetrics,
    val motionMetrics: MotionMetrics,
    val adhdAssessment: ADHDAssessmentResult
)

class GoNoGoTaskViewModel(
    private val context: Context
) : BaseAssessmentTaskViewModel() {

    private val adhdAnalyzer = ADHDAnalyzer()

    // Task UI state
    private val _uiState = MutableStateFlow<GoNoGoTaskState>(GoNoGoTaskState.Instructions)
    val uiState: StateFlow<GoNoGoTaskState> = _uiState.asStateFlow()

    // Task parameters
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
    private var stimulusShownTime = 0L

    // Task parameters
    private var stimulusTimer: CountDownTimer? = null

    init {
        // Just initialize the services without starting them
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.resetTracking()
        }
    }

    fun startCountdown() {
        _uiState.value = GoNoGoTaskState.Countdown(3)

        super.startCountdown(3) {
            startTask()
        }
    }

    override fun startTask() {
        // Reset metrics
        correctGo = 0
        correctNoGo = 0
        missedGo = 0
        incorrectNoGo = 0
        goResponseTimes.clear()

        // Start sensors
        startSensors()

        // Set up task state
        _uiState.value = GoNoGoTaskState.Running

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
        _stimulusType.value = null

        // Cancel any existing stimulus timer
        stimulusTimer?.cancel()

        // Random delay between stimuli (1000-2000ms)
        val delay = Random.nextInt(1000, 2001).toLong()

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

        // Calculate response time
        val responseTime = System.currentTimeMillis() - stimulusShownTime

        when (_stimulusType.value) {
            StimulusType.GO -> {
                // Correct Go response
                correctGo++

                // Record response time
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

    override fun completeTask() {
        // Clean up timers
        mainTimer?.cancel()
        stimulusTimer?.cancel()
        sensorUpdateTimer?.cancel()

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

        // Calculate response time variability (standard deviation)
        val responseTimeVariability = if (goResponseTimes.size > 1) {
            val mean = goResponseTimes.average()
            val variance = goResponseTimes.map { (it - mean) * (it - mean) }.sum() / goResponseTimes.size
            sqrt(variance).toFloat()
        } else {
            0f
        }

        // Analyze ADHD indicators
        val adhdAssessment = adhdAnalyzer.analyzePerformance(
            correctResponses = correctGo,
            incorrectResponses = incorrectNoGo,
            missedResponses = missedGo,
            averageResponseTime = averageResponseTime,
            responseTimeVariability = responseTimeVariability,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            durationSeconds = taskDuration
        )

        // Create domain entity for storage in repository
        val domainResult = DomainGoNoGoTaskResult(
            correctGo = correctGo,
            correctNoGo = correctNoGo,
            missedGo = missedGo,
            incorrectNoGo = incorrectNoGo,
            accuracy = accuracy,
            averageResponseTime = averageResponseTime,
            adhdProbabilityScore = adhdAssessment.adhdProbabilityScore
        )

        // Save to repository
        resultsRepository.saveGoNoGoResult(domainResult)

        // Create presentation model for UI (matching CPT format)
        val uiResult = GoNoGoTaskResultUI(
            correctGo = correctGo,
            correctNoGo = correctNoGo,
            missedGo = missedGo,
            incorrectNoGo = incorrectNoGo,
            averageResponseTime = averageResponseTime,
            accuracy = accuracy,
            responseTimesMs = goResponseTimes,
            responseTimeVariability = responseTimeVariability,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            adhdAssessment = adhdAssessment
        )

        // Update UI state with the presentation model
        _uiState.value = GoNoGoTaskState.Completed(uiResult)
    }

    fun processFaceImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (_uiState.value == GoNoGoTaskState.Running) {
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
            if (modelClass.isAssignableFrom(GoNoGoTaskViewModel::class.java)) {
                return GoNoGoTaskViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}