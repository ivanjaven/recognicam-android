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

    // Advanced thresholds - Significantly increased to reduce sensitivity
    private val NOISE_THRESHOLD = 0.09f      // Was 0.07f
    private val FIDGET_THRESHOLD = 0.15f     // Was 0.12f
    private val MEDIUM_MOVEMENT_THRESHOLD = 0.5f  // Was 0.45f
    private val LARGE_MOVEMENT_THRESHOLD = 1.4f    // Was 1.2f
    private val SUDDEN_MOVEMENT_THRESHOLD = 2.0f   // Was 1.8f

    // Maximum counts to prevent inflated values
    private val MAX_DIRECTION_CHANGES = 100
    private val MAX_SUDDEN_MOVEMENTS = 40

    // Window size for moving average filter
    private val FILTER_WINDOW_SIZE = 7  // Increased for smoother filtering
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

        // Debug logging
        println("Motion tracking started: $isTrackingActive")
    }

    fun stopTracking() {
        if (!isTrackingActive) return

        sensorManager.unregisterListener(this)
        isTrackingActive = false

        // Debug logging
        println("Motion tracking stopped")
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

        // Debug logging
        println("Motion tracking reset")
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

            // Update metrics every data point for more responsive updates
            calculateAndUpdateMetrics()
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
            var lastDirectionChangeTime = 0L

            // Increased time thresholds for less sensitivity
            val MIN_DIRECTION_CHANGE_INTERVAL = 200L // Was 100L, increased delay between direction changes
            var lastSuddenMovementTime = 0L
            val MIN_SUDDEN_MOVEMENT_INTERVAL = 500L // Was 300L, increased delay between sudden movements

            // Analyze each data point
            for (point in motionData) {
                // Classify movement intensity
                when {
                    point.magnitude >= LARGE_MOVEMENT_THRESHOLD -> largeMovementFrames++
                    point.magnitude >= MEDIUM_MOVEMENT_THRESHOLD -> mediumMovementFrames++
                    point.magnitude >= FIDGET_THRESHOLD -> fidgetFrames++
                }

                // Count sudden movements with stricter time-based filtering
                if (point.magnitude >= SUDDEN_MOVEMENT_THRESHOLD) {
                    val now = point.timestamp
                    if (now - lastSuddenMovementTime > MIN_SUDDEN_MOVEMENT_INTERVAL &&
                        suddenMovements < MAX_SUDDEN_MOVEMENTS) {
                        suddenMovements++
                        lastSuddenMovementTime = now
                    }
                }

                // Count direction changes with stricter time-based filtering
                val currDirection = Triple(
                    if (point.x > NOISE_THRESHOLD) 1 else if (point.x < -NOISE_THRESHOLD) -1 else 0,
                    if (point.y > NOISE_THRESHOLD) 1 else if (point.y < -NOISE_THRESHOLD) -1 else 0,
                    if (point.z > NOISE_THRESHOLD) 1 else if (point.z < -NOISE_THRESHOLD) -1 else 0
                )

                // More strict direction change detection - require bigger changes
                if (prevDirection != Triple(0, 0, 0) && currDirection != Triple(0, 0, 0) &&
                    (currDirection.first != 0 && prevDirection.first != 0 && currDirection.first != prevDirection.first ||
                            currDirection.second != 0 && prevDirection.second != 0 && currDirection.second != prevDirection.second ||
                            currDirection.third != 0 && prevDirection.third != 0 && currDirection.third != prevDirection.third)) {

                    val now = point.timestamp
                    if (now - lastDirectionChangeTime > MIN_DIRECTION_CHANGE_INTERVAL &&
                        directionChanges < MAX_DIRECTION_CHANGES) {
                        directionChanges++
                        lastDirectionChangeTime = now
                    }
                }

                if (currDirection != Triple(0, 0, 0)) {
                    prevDirection = currDirection
                }

                totalMagnitude += point.magnitude
            }

            // Calculate scores with better normalization and less sensitivity
            // Apply a scaling factor to normalize fidgeting and reduce overall sensitivity
            val normalizedFidgetFrames = (fidgetFrames * 0.9f).toInt() // Scale down fidget counts
            val fidgetingScore = ((normalizedFidgetFrames * 100.0 / totalFrames) * 0.9f).toInt().coerceIn(0, 100)

            // Calculate general movement with similar scaling
            val generalMovementScore =
                (((fidgetFrames + mediumMovementFrames + largeMovementFrames) * 100.0 / totalFrames) * 0.85f)
                    .toInt().coerceIn(0, 100)

            val movementIntensity = (totalMagnitude / totalFrames) * 0.9f

            // Better calculation for restlessness score with balanced influence
            // Normalize direction changes to prevent extremes
            val normalizedDirectionChanges = (directionChanges * 80.0 / MAX_DIRECTION_CHANGES).toInt()
            val normalizedSuddenMovements = (suddenMovements * 80.0 / MAX_SUDDEN_MOVEMENTS).toInt()

            val directionChangeRate = (normalizedDirectionChanges * 12.0 / totalFrames).toInt()  // Was 15.0
            val suddenMovementRate = (normalizedSuddenMovements * 15.0 / totalFrames).toInt()    // Was 20.0
            val fidgetRate = (fidgetingScore * 0.5f).toInt() // Was 0.6f, reduced influence

            // Combine factors with caps to prevent extreme values
            val restlessness = ((directionChangeRate + suddenMovementRate + fidgetRate) * 0.85f).toInt().coerceIn(0, 100)

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
        // Log final metrics
        val metrics = analyzeMotion()
        println("Final motion metrics - Fidgeting: ${metrics.fidgetingScore}%, Restlessness: ${metrics.restlessness}%")
        return metrics
    }
}