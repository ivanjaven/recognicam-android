// CPTTaskViewModel with sensor integration
package com.example.recognicam.presentation.viewmodel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.data.analysis.ADHDAnalyzer
import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.sensor.MotionDetectionService
import com.example.recognicam.data.sensor.FaceAnalysisService
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import com.example.recognicam.domain.entity.CPTTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt
import kotlin.random.Random

class CPTTaskViewModel(
    private val context: Context
) : ViewModel() {

    private val resultsRepository = ServiceLocator.getResultsRepository()
    private val motionDetectionService = MotionDetectionService(context)
    private val faceAnalysisService = FaceAnalysisService()
    private val adhdAnalyzer = ADHDAnalyzer()

    // Task UI state
    private val _uiState = MutableStateFlow<CPTTaskState>(CPTTaskState.Instructions)
    val uiState: StateFlow<CPTTaskState> = _uiState.asStateFlow()

    // Task parameters
    private val _timeRemaining = MutableStateFlow(40) // Default 40 seconds
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _stimulus = MutableStateFlow<Char?>(null)
    val stimulus: StateFlow<Char?> = _stimulus.asStateFlow()

    private val _stimulusVisible = MutableStateFlow(false)
    val stimulusVisible: StateFlow<Boolean> = _stimulusVisible.asStateFlow()

    private val _isTargetStimulus = MutableStateFlow(false)
    val isTargetStimulus: StateFlow<Boolean> = _isTargetStimulus.asStateFlow()

    // Sensor metrics streams
    val faceMetrics: StateFlow<FaceMetrics> = faceAnalysisService.faceMetrics
    val motionMetrics: StateFlow<MotionMetrics> = motionDetectionService.motionMetrics

    // Performance metrics
    private var correctResponses = 0
    private var incorrectResponses = 0
    private var missedResponses = 0
    private val responseTimes = mutableListOf<Long>()
    private var stimulusShownTime = 0L

    // Task parameters
    private val letters = listOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L')
    private val targetLetter = 'X'
    private var taskDuration = 40 // seconds

    // Timers
    private var mainTimer: CountDownTimer? = null
    private var stimulusTimer: CountDownTimer? = null

    init {
        // Start tracking motion when ViewModel is created
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }
    }

    fun startCountdown() {
        _uiState.value = CPTTaskState.Countdown(3)

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                _uiState.value = CPTTaskState.Countdown(secondsLeft.toInt())
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

        // Reset sensors
        motionDetectionService.resetTracking()
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }

        faceAnalysisService.reset()
        faceAnalysisService.start()

        // Set up task state
        _uiState.value = CPTTaskState.Running
        _timeRemaining.value = taskDuration

        // Start main timer
        mainTimer = object : CountDownTimer(taskDuration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemaining.value = (millisUntilFinished / 1000).toInt()

                // Update sensor metrics periodically
                motionDetectionService.calculateAndUpdateMetrics()
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
        _stimulus.value = null

        // Cancel any existing stimulus timer
        stimulusTimer?.cancel()

        // Random delay between stimuli (500-1500ms)
        val delay = Random.nextInt(500, 1501).toLong()

        stimulusTimer = object : CountDownTimer(delay, delay) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Determine if this should be a target (20% chance)
                val isTarget = Random.nextDouble() < 0.2

                if (isTarget) {
                    _stimulus.value = targetLetter
                    _isTargetStimulus.value = true
                } else {
                    _stimulus.value = letters.random()
                    _isTargetStimulus.value = false
                }

                // Show the stimulus
                stimulusShownTime = System.currentTimeMillis()
                _stimulusVisible.value = true

                // Set timer for missed response (1.5 seconds)
                stimulusTimer = object : CountDownTimer(1500, 1500) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        // If stimulus is still visible (no response)
                        if (_stimulusVisible.value && _isTargetStimulus.value) {
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
        if (_uiState.value != CPTTaskState.Running || !_stimulusVisible.value) {
            return
        }

        // Calculate response time
        val responseTime = System.currentTimeMillis() - stimulusShownTime

        if (_isTargetStimulus.value) {
            // Correct response
            correctResponses++
            responseTimes.add(responseTime)
        } else {
            // Incorrect response
            incorrectResponses++
        }

        // Cancel the stimulus timer to prevent missed response counting
        stimulusTimer?.cancel()

        // Hide the stimulus
        _stimulusVisible.value = false

        // Move to next stimulus
        presentNextStimulus()
    }

    private fun completeTask() {
        // Clean up timers
        mainTimer?.cancel()
        stimulusTimer?.cancel()

        // Get final sensor metrics
        val finalFaceMetrics = faceAnalysisService.getFinalMetrics()
        val finalMotionMetrics = motionDetectionService.getFinalMetrics()

        // Stop tracking
        if (motionDetectionService.isTracking()) {
            motionDetectionService.stopTracking()
        }

        // Calculate response time variability (standard deviation)
        val responseTimeVariability = if (responseTimes.size > 1) {
            val mean = responseTimes.average()
            val variance = responseTimes.map { (it - mean) * (it - mean) }.sum() / responseTimes.size
            sqrt(variance).toFloat()
        } else {
            0f
        }

        // Calculate average response time
        val averageResponseTime = if (responseTimes.isNotEmpty()) {
            responseTimes.average().toInt()
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

        // Create result with enhanced metrics
        val result = EnhancedCPTTaskResult(
            correctResponses = correctResponses,
            incorrectResponses = incorrectResponses,
            missedResponses = missedResponses,
            responseTimesMs = responseTimes,
            accuracy = calculateAccuracy(correctResponses, incorrectResponses, missedResponses),
            averageResponseTime = averageResponseTime,
            responseTimeVariability = responseTimeVariability,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            adhdAssessment = adhdAssessment
        )

        // Save result (convert to basic result for repository if needed)
        resultsRepository.saveCPTResult(
            CPTTaskResult(
                correctResponses = correctResponses,
                incorrectResponses = incorrectResponses,
                missedResponses = missedResponses,
                accuracy = result.accuracy,
                averageResponseTime = averageResponseTime,
                fidgetingScore = finalMotionMetrics.fidgetingScore,
                generalMovementScore = finalMotionMetrics.generalMovementScore,
                directionChanges = finalMotionMetrics.directionChanges,
                adhdProbabilityScore = adhdAssessment.adhdProbabilityScore
            )
        )

        // Update UI state
        _uiState.value = CPTTaskState.Completed(result)
    }

    private fun calculateAccuracy(correct: Int, incorrect: Int, missed: Int): Int {
        val totalTargets = correct + missed
        val totalResponses = correct + incorrect

        // Avoid division by zero
        if (totalTargets == 0 || totalResponses == 0) return 0

        // Balanced accuracy calculation
        val hitRate = correct.toFloat() / totalTargets
        val correctRejectionRate = 1 - (incorrect.toFloat() / totalResponses)

        return ((hitRate + correctRejectionRate) / 2 * 100).toInt()
    }

    fun setTaskDuration(seconds: Int) {
        if (_uiState.value == CPTTaskState.Instructions) {
            taskDuration = seconds
        }
    }

    fun processFaceImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (_uiState.value == CPTTaskState.Running) {
            faceAnalysisService.processImage(imageProxy)
        } else {
            imageProxy.close()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainTimer?.cancel()
        stimulusTimer?.cancel()

        if (motionDetectionService.isTracking()) {
            motionDetectionService.stopTracking()
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CPTTaskViewModel::class.java)) {
                return CPTTaskViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class EnhancedCPTTaskResult(
    val correctResponses: Int,
    val incorrectResponses: Int,
    val missedResponses: Int,
    val responseTimesMs: List<Long>,
    val accuracy: Int,
    val averageResponseTime: Int,
    val responseTimeVariability: Float,
    val faceMetrics: FaceMetrics,
    val motionMetrics: MotionMetrics,
    val adhdAssessment: ADHDAssessmentResult
)

sealed class CPTTaskState {
    object Instructions : CPTTaskState()
    data class Countdown(val count: Int) : CPTTaskState()
    object Running : CPTTaskState()
    data class Completed(val result: EnhancedCPTTaskResult) : CPTTaskState()
}