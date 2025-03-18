// EnhancedMotionDetectionService.kt
package com.example.recognicam.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class MotionDataPoint(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float
)

data class MotionMetrics(
    val fidgetingScore: Int = 0,
    val generalMovementScore: Int = 0,
    val directionChanges: Int = 0,
    val suddenMovements: Int = 0,
    val movementIntensity: Float = 0f,
    val restlessness: Int = 0 // 0-100 scale
)

class MotionDetectionService(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isTrackingActive = false
    private val motionData = mutableListOf<MotionDataPoint>()

    // Last values for calculations
    private var lastAcceleration = Triple(0f, 0f, 0f)
    private var movingAverageX = 0f
    private var movingAverageY = 0f
    private var movingAverageZ = 0f

    // Advanced thresholds
    private val NOISE_THRESHOLD = 0.02f
    private val FIDGET_THRESHOLD = 0.05f
    private val MEDIUM_MOVEMENT_THRESHOLD = 0.3f
    private val LARGE_MOVEMENT_THRESHOLD = 0.8f
    private val SUDDEN_MOVEMENT_THRESHOLD = 1.2f

    // Window size for moving average filter
    private val FILTER_WINDOW_SIZE = 5
    private val recentReadings = mutableListOf<Triple<Float, Float, Float>>()

    // State flow for real-time updates
    private val _motionMetrics = MutableStateFlow(MotionMetrics())
    val motionMetrics: StateFlow<MotionMetrics> = _motionMetrics.asStateFlow()

    fun startTracking() {
        if (isTrackingActive) return

        motionData.clear()
        recentReadings.clear()
        isTrackingActive = true
        lastAcceleration = Triple(0f, 0f, 0f)
        movingAverageX = 0f
        movingAverageY = 0f
        movingAverageZ = 0f

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME // ~50Hz sampling rate
        )

        _motionMetrics.value = MotionMetrics()
    }

    fun stopTracking() {
        if (!isTrackingActive) return

        sensorManager.unregisterListener(this)
        isTrackingActive = false
    }

    fun isTracking(): Boolean = isTrackingActive

    fun resetTracking() {
        motionData.clear()
        recentReadings.clear()
        lastAcceleration = Triple(0f, 0f, 0f)
        movingAverageX = 0f
        movingAverageY = 0f
        movingAverageZ = 0f
        _motionMetrics.value = MotionMetrics()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTrackingActive || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Apply moving average filter to reduce noise
        updateMovingAverage(x, y, z)

        // Skip the first few readings until filter is initialized
        if (recentReadings.size < FILTER_WINDOW_SIZE) {
            lastAcceleration = Triple(movingAverageX, movingAverageY, movingAverageZ)
            return
        }

        // Calculate differential using filtered values
        val diffX = movingAverageX - lastAcceleration.first
        val diffY = movingAverageY - lastAcceleration.second
        val diffZ = movingAverageZ - lastAcceleration.third

        // Calculate magnitude of change
        val magnitude = sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ)

        // Only record if movement is above noise threshold
        if (magnitude > NOISE_THRESHOLD) {
            // Store data
            synchronized(motionData) {
                motionData.add(
                    MotionDataPoint(
                        timestamp = System.currentTimeMillis(),
                        x = diffX,
                        y = diffY,
                        z = diffZ,
                        magnitude = magnitude
                    )
                )
            }

            // Update metrics in real-time (every 10 data points to avoid excessive calculations)
            if (motionData.size % 10 == 0) {
                calculateAndUpdateMetrics()
            }
        }

        // Update last values for next calculation
        lastAcceleration = Triple(movingAverageX, movingAverageY, movingAverageZ)
    }

    private fun updateMovingAverage(x: Float, y: Float, z: Float) {
        recentReadings.add(Triple(x, y, z))

        // Keep only the most recent readings
        if (recentReadings.size > FILTER_WINDOW_SIZE) {
            recentReadings.removeAt(0)
        }

        // Calculate moving averages
        if (recentReadings.isNotEmpty()) {
            movingAverageX = recentReadings.sumOf { it.first.toDouble() }.toFloat() / recentReadings.size
            movingAverageY = recentReadings.sumOf { it.second.toDouble() }.toFloat() / recentReadings.size
            movingAverageZ = recentReadings.sumOf { it.third.toDouble() }.toFloat() / recentReadings.size
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    fun calculateAndUpdateMetrics() {
        if (motionData.size < 5) {
            _motionMetrics.value = MotionMetrics()
            return
        }

        val metrics = analyzeMotion()
        _motionMetrics.value = metrics
    }

    fun analyzeMotion(): MotionMetrics {
        if (motionData.size < 5) {
            return MotionMetrics()
        }

        synchronized(motionData) {
            val totalFrames = motionData.size
            var fidgetFrames = 0
            var mediumMovementFrames = 0
            var largeMovementFrames = 0
            var directionChanges = 0
            var suddenMovements = 0
            var totalMagnitude = 0f
            var prevDirection = Triple(0, 0, 0)

            // Analyze each data point
            for (point in motionData) {
                // Classify movement intensity
                when {
                    point.magnitude >= LARGE_MOVEMENT_THRESHOLD -> largeMovementFrames++
                    point.magnitude >= MEDIUM_MOVEMENT_THRESHOLD -> mediumMovementFrames++
                    point.magnitude >= FIDGET_THRESHOLD -> fidgetFrames++
                }

                // Count sudden movements
                if (point.magnitude >= SUDDEN_MOVEMENT_THRESHOLD) {
                    suddenMovements++
                }

                // Count direction changes
                val currDirection = Triple(
                    if (point.x > NOISE_THRESHOLD) 1 else if (point.x < -NOISE_THRESHOLD) -1 else 0,
                    if (point.y > NOISE_THRESHOLD) 1 else if (point.y < -NOISE_THRESHOLD) -1 else 0,
                    if (point.z > NOISE_THRESHOLD) 1 else if (point.z < -NOISE_THRESHOLD) -1 else 0
                )

                if (prevDirection != Triple(0, 0, 0) && currDirection != Triple(0, 0, 0) &&
                    (currDirection.first != 0 && prevDirection.first != 0 && currDirection.first != prevDirection.first ||
                            currDirection.second != 0 && prevDirection.second != 0 && currDirection.second != prevDirection.second ||
                            currDirection.third != 0 && prevDirection.third != 0 && currDirection.third != prevDirection.third)) {
                    directionChanges++
                }

                if (currDirection != Triple(0, 0, 0)) {
                    prevDirection = currDirection
                }

                totalMagnitude += point.magnitude
            }

            // Calculate scores
            val fidgetingScore = (fidgetFrames * 100.0 / totalFrames).toInt().coerceIn(0, 100)
            val generalMovementScore = ((fidgetFrames + mediumMovementFrames + largeMovementFrames) * 100.0 / totalFrames).toInt().coerceIn(0, 100)
            val movementIntensity = (totalMagnitude / totalFrames)

            // Calculate restlessness score (combination of factors)
            val directionChangeRate = (directionChanges * 20.0 / totalFrames).toInt()
            val suddenMovementRate = (suddenMovements * 30.0 / totalFrames).toInt()
            val fidgetRate = (fidgetingScore * 0.5).toInt()

            val restlessness = (directionChangeRate + suddenMovementRate + fidgetRate).coerceIn(0, 100)

            return MotionMetrics(
                fidgetingScore = fidgetingScore,
                generalMovementScore = generalMovementScore,
                directionChanges = directionChanges,
                suddenMovements = suddenMovements,
                movementIntensity = movementIntensity,
                restlessness = restlessness
            )
        }
    }

    fun getFinalMetrics(): MotionMetrics {
        return analyzeMotion()
    }
}