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

    // ADJUSTED THRESHOLDS for fidget detection - less sensitive to very small movements
    private val NOISE_THRESHOLD = 0.08f           // Was 0.07f - less sensitive to very small movements
    private val FIDGET_THRESHOLD = 0.13f          // Was 0.11f - less sensitive to very small movements
    private val MEDIUM_MOVEMENT_THRESHOLD = 0.35f // Unchanged
    private val LARGE_MOVEMENT_THRESHOLD = 0.9f   // Unchanged
    private val SUDDEN_MOVEMENT_THRESHOLD = 1.4f  // Unchanged

    // Maximum counts to prevent inflated values
    private val MAX_DIRECTION_CHANGES = 120
    private val MAX_SUDDEN_MOVEMENTS = 50

    // Window size for moving average filter
    private val FILTER_WINDOW_SIZE = 6
    private val recentReadings = mutableListOf<Triple<Float, Float, Float>>()

    // Fidget pattern detection - adjusted to be less sensitive to tiny movements
    private val fidgetPatternWindow = mutableListOf<Pair<Long, Float>>()
    private val FIDGET_PATTERN_WINDOW_SIZE = 100
    private var fidgetPatternScore = 0

    // State flow for real-time updates
    private val _motionMetrics = MutableStateFlow(MotionMetrics())
    val motionMetrics: StateFlow<MotionMetrics> = _motionMetrics.asStateFlow()

    fun startTracking() {
        if (isTrackingActive) return

        motionData.clear()
        recentReadings.clear()
        fidgetPatternWindow.clear()
        fidgetPatternScore = 0
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
        fidgetPatternWindow.clear()
        fidgetPatternScore = 0
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

        // Track movement for fidget pattern detection with higher threshold for tiny movements
        if (magnitude > NOISE_THRESHOLD) {
            fidgetPatternWindow.add(Pair(System.currentTimeMillis(), magnitude))

            // Keep the window size limited
            if (fidgetPatternWindow.size > FIDGET_PATTERN_WINDOW_SIZE) {
                fidgetPatternWindow.removeAt(0)
            }

            // Analyze fidget patterns periodically
            if (fidgetPatternWindow.size > 20 && fidgetPatternWindow.size % 15 == 0) {
                detectFidgetPatterns()
            }
        }

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

    // Adjusted fidget pattern detection - focusing on more significant fidgeting
    private fun detectFidgetPatterns() {
        if (fidgetPatternWindow.size < 20) return

        // Look for rhythmic movements in the fidget range (not extremely small)
        var rhythmCount = 0
        var fidgetMovementCount = 0

        // Count fidget movements - only those above FIDGET_THRESHOLD
        for (i in fidgetPatternWindow.indices) {
            val (_, magnitude) = fidgetPatternWindow[i]
            if (magnitude in FIDGET_THRESHOLD..MEDIUM_MOVEMENT_THRESHOLD) {
                fidgetMovementCount++
            }
        }

        // Check for rhythmic patterns - looking for actual fidget patterns
        val intervals = mutableListOf<Long>()
        for (i in 1 until fidgetPatternWindow.size) {
            intervals.add(fidgetPatternWindow[i].first - fidgetPatternWindow[i-1].first)
        }

        // Group similar intervals (within 80ms of each other)
        val intervalGroups = mutableMapOf<Long, Int>()
        for (interval in intervals) {
            val groupKey = (interval / 80) * 80 // Group by 80ms buckets
            intervalGroups[groupKey] = (intervalGroups[groupKey] ?: 0) + 1
        }

        // If any interval pattern repeats multiple times, count it as rhythmic
        for ((_, count) in intervalGroups) {
            if (count >= 3) { // At least 3 similar-timed movements is a pattern
                rhythmCount += count
            }
        }

        // Calculate a fidget pattern score with improved weighting
        val fidgetMovementRatio = fidgetMovementCount.toFloat() / fidgetPatternWindow.size
        val rhythmRatio = rhythmCount.toFloat() / intervals.size.coerceAtLeast(1)

        // Calculate score with balanced weights
        fidgetPatternScore = ((fidgetMovementRatio * 0.35f + rhythmRatio * 0.45f) * 100).toInt().coerceIn(0, 100)
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

            // Balanced time thresholds
            val MIN_DIRECTION_CHANGE_INTERVAL = 180L
            var lastSuddenMovementTime = 0L
            val MIN_SUDDEN_MOVEMENT_INTERVAL = 400L

            // Analyze each data point
            for (point in motionData) {
                // Classify movement intensity
                when {
                    point.magnitude >= LARGE_MOVEMENT_THRESHOLD -> largeMovementFrames++
                    point.magnitude >= MEDIUM_MOVEMENT_THRESHOLD -> mediumMovementFrames++
                    point.magnitude >= FIDGET_THRESHOLD -> fidgetFrames++ // Only counts actual fidgeting, not tiny movements
                }

                // Count sudden movements
                if (point.magnitude >= SUDDEN_MOVEMENT_THRESHOLD) {
                    val now = point.timestamp
                    if (now - lastSuddenMovementTime > MIN_SUDDEN_MOVEMENT_INTERVAL &&
                        suddenMovements < MAX_SUDDEN_MOVEMENTS) {
                        suddenMovements++
                        lastSuddenMovementTime = now
                    }
                }

                // Count direction changes
                val currDirection = Triple(
                    if (point.x > NOISE_THRESHOLD) 1 else if (point.x < -NOISE_THRESHOLD) -1 else 0,
                    if (point.y > NOISE_THRESHOLD) 1 else if (point.y < -NOISE_THRESHOLD) -1 else 0,
                    if (point.z > NOISE_THRESHOLD) 1 else if (point.z < -NOISE_THRESHOLD) -1 else 0
                )

                // Detect direction changes
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

            // ADJUSTED SCORE CALCULATIONS - less sensitive to tiny movements

            // Fidgeting score - focusing on actual fidgeting, not tiny movements
            val smallMovementsRatio = fidgetFrames.toFloat() / totalFrames.coerceAtLeast(1)

            // Use pattern detection combined with raw fidget count
            val rawFidgetScore = (smallMovementsRatio * 100f).toInt()

            // Combine pattern detection with raw count for more accurate assessment
            val combinedFidgetScore = if (fidgetPatternScore > 0) {
                (rawFidgetScore * 0.5f + fidgetPatternScore * 0.5f).toInt()
            } else {
                (rawFidgetScore * 0.9f).toInt()
            }

            // Final fidgeting score - adjusted to be less sensitive to tiny movements
            val fidgetingScore = if (combinedFidgetScore > 25) {
                (combinedFidgetScore * 0.95f).toInt().coerceIn(0, 100) // Scale down scores above threshold
            } else {
                (combinedFidgetScore * 0.7f).toInt() // Reduce sensitivity for very small movements
            }

            // General movement score - focusing on larger movements
            val generalMovementScore =
                (((mediumMovementFrames + largeMovementFrames * 1.5) * 100.0 / totalFrames) * 0.9f)
                    .toInt().coerceIn(0, 100)

            // Movement intensity - average magnitude
            val movementIntensity = (totalMagnitude / totalFrames) * 0.9f

            // Restlessness - focused on larger body movements, distinct from fidgeting
            val largeMovementFactor = (largeMovementFrames * 100f / totalFrames.coerceAtLeast(1)) * 0.5f
            val suddenMovementFactor = (suddenMovements * 100f / MAX_SUDDEN_MOVEMENTS.coerceAtLeast(1)) * 0.2f
            val directionChangeFactor = (directionChanges * 100f / MAX_DIRECTION_CHANGES.coerceAtLeast(1)) * 0.2f

            // Combine factors for restlessness score
            val restlessness = ((largeMovementFactor + suddenMovementFactor + directionChangeFactor) * 0.95f)
                .toInt().coerceIn(0, 100)

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