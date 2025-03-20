package com.example.recognicam.core.base

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseAssessmentTaskViewModel : ViewModel() {

    protected val resultsRepository = ServiceLocator.getResultsRepository()
    protected val motionDetectionService = ServiceLocator.getMotionDetectionService()
    protected val faceAnalysisService = ServiceLocator.getFaceDetectionService()

    // Make property private with explicit setter method
    private var _taskDuration = 30 // Default duration in seconds
    protected val taskDuration: Int get() = _taskDuration

    // Renamed to avoid clash with auto-generated property setter
    fun configureDuration(seconds: Int) {
        _taskDuration = seconds
    }

    // Common UI states
    protected val _timeRemaining = MutableStateFlow(30)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    // Sensor metrics
    protected val _faceMetrics = MutableStateFlow(FaceMetrics())
    val faceMetrics: StateFlow<FaceMetrics> = _faceMetrics.asStateFlow()

    protected val _motionMetrics = MutableStateFlow(MotionMetrics())
    val motionMetrics: StateFlow<MotionMetrics> = _motionMetrics.asStateFlow()

    // Common timers
    protected var mainTimer: CountDownTimer? = null
    protected var sensorUpdateTimer: CountDownTimer? = null

    // Abstract methods that each task must implement
    abstract fun startTask()
    abstract fun completeTask()

    protected fun startCountdown(countdownSeconds: Int = 3, onComplete: () -> Unit) {
        object : CountDownTimer(countdownSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // To be implemented in specific viewmodels
            }

            override fun onFinish() {
                onComplete()
            }
        }.start()
    }

    protected fun startMainTimer() {
        _timeRemaining.value = _taskDuration

        mainTimer = object : CountDownTimer(_taskDuration * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemaining.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                completeTask()
            }
        }.start()
    }

    protected fun startSensors() {
        // Reset and start face analysis
        faceAnalysisService.reset()
        faceAnalysisService.start()

        // Reset and start motion detection
        motionDetectionService.resetTracking()
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.startTracking()
        }

        println("Sensors started - motion tracking: ${motionDetectionService.isTracking()}")
    }

    protected fun startSensorUpdateTimer() {
        // Cancel any existing timer
        sensorUpdateTimer?.cancel()

        // Create a timer that updates sensor data every 500ms
        sensorUpdateTimer = object : CountDownTimer(_taskDuration * 1000L, 500) {
            override fun onTick(millisUntilFinished: Long) {
                updateSensorMetrics()
            }

            override fun onFinish() {
                // Final update
                updateSensorMetrics()
            }
        }.start()
    }

    protected fun updateSensorMetrics() {
        // Force manual updates and copy to local state
        motionDetectionService.calculateAndUpdateMetrics()
        _motionMetrics.value = motionDetectionService.getFinalMetrics()
        _faceMetrics.value = faceAnalysisService.getFinalMetrics()
    }

    override fun onCleared() {
        super.onCleared()
        mainTimer?.cancel()
        sensorUpdateTimer?.cancel()

        if (motionDetectionService.isTracking()) {
            motionDetectionService.stopTracking()
        }
    }
}