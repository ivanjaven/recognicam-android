package com.example.recognicam.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

data class MotionDataPoint(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float
)

data class MotionMetrics(
    val fidgetingScore: Int = 0,           // 0-100 scale
    val generalMovementScore: Int = 0,     // 0-100 scale
    val directionChanges: Int = 0,         // Count
    val suddenMovements: Int = 0,          // Count
    val movementIntensity: Float = 0f,     // Raw value
    val restlessness: Int = 0              // 0-100 scale
)

class MotionDetectionService(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isTrackingActive = false
    private val motionData = mutableListOf<MotionDataPoint>()
    private val rotationData = mutableListOf<MotionDataPoint>() // For gyroscope data

    // Last values for calculations
    private var lastAcceleration = Triple(0f, 0f, 0f)
    private var movingAverageX = 0f
    private var movingAverageY = 0f
    private var movingAverageZ = 0f

    // Improved thresholds for better detection
    private val NOISE_THRESHOLD = 0.07f           // Small movements below this are considered noise
    private val FIDGET_THRESHOLD = 0.12f          // Movements this size likely fidgeting
    private val MEDIUM_MOVEMENT_THRESHOLD = 0.4f  // Medium movements
    private val LARGE_MOVEMENT_THRESHOLD = 0.8f   // Large movements
    private val SUDDEN_MOVEMENT_THRESHOLD = 1.3f  // Sudden/fast movements

    // Repetitive movement detection (fidgeting)
    private val REPETITIVE_PATTERN_WINDOW = 3000L // 3 second window to detect repetitive movements
    private val SIMILAR_DIRECTION_THRESHOLD = 0.7f // How similar movements need to be to count as repetitive

    // Maximum counts to prevent inflated values
    private val MAX_DIRECTION_CHANGES = 150
    private val MAX_SUDDEN_MOVEMENTS = 70

    // Window size for moving average filter
    private val FILTER_WINDOW_SIZE = 6
    private val recentReadings = mutableListOf<Triple<Float, Float, Float>>()

    // Fidget pattern improved detection
    private val fidgetPatternWindow = mutableListOf<Pair<Long, Triple<Float, Float, Float>>>() // Timestamp and movement vector
    private val FIDGET_PATTERN_WINDOW_SIZE = 150
    private var fidgetPatternScore = 0
    private var repetitiveMovementCount = 0
    private var lastTimeRepeatedDirection = 0L
    private var lastRepetitiveDirection = Triple(0f, 0f, 0f)

    // Timestamp of session start
    private var sessionStartTime = 0L

    // State flow for real-time updates
    private val _motionMetrics = MutableStateFlow(MotionMetrics())
    val motionMetrics: StateFlow<MotionMetrics> = _motionMetrics.asStateFlow()

    fun startTracking() {
        if (isTrackingActive) return

        motionData.clear()
        rotationData.clear()
        recentReadings.clear()
        fidgetPatternWindow.clear()
        fidgetPatternScore = 0
        repetitiveMovementCount = 0
        isTrackingActive = true
        lastAcceleration = Triple(0f, 0f, 0f)
        movingAverageX = 0f
        movingAverageY = 0f
        movingAverageZ = 0f
        sessionStartTime = System.currentTimeMillis()

        // Register both accelerometer and gyroscope
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME // ~50Hz sampling rate
        )

        if (gyroscope != null) {
            sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        _motionMetrics.value = MotionMetrics()

        println("Motion tracking started: $isTrackingActive")
    }

    fun stopTracking() {
        if (!isTrackingActive) return

        sensorManager.unregisterListener(this)
        isTrackingActive = false

        println("Motion tracking stopped")
    }

    fun isTracking(): Boolean = isTrackingActive

    fun resetTracking() {
        motionData.clear()
        rotationData.clear()
        recentReadings.clear()
        fidgetPatternWindow.clear()
        fidgetPatternScore = 0
        repetitiveMovementCount = 0
        lastAcceleration = Triple(0f, 0f, 0f)
        movingAverageX = 0f
        movingAverageY = 0f
        movingAverageZ = 0f
        sessionStartTime = System.currentTimeMillis()
        _motionMetrics.value = MotionMetrics()

        println("Motion tracking reset")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTrackingActive) return

        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometerData(event)
            Sensor.TYPE_GYROSCOPE -> processGyroscopeData(event)
        }
    }

    private fun processAccelerometerData(event: SensorEvent) {
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

        // Track movement for fidget pattern detection
        if (magnitude > NOISE_THRESHOLD) {
            // Add to pattern window with direction vector
            fidgetPatternWindow.add(Pair(
                System.currentTimeMillis(),
                Triple(diffX, diffY, diffZ)
            ))

            // Keep the window size limited
            if (fidgetPatternWindow.size > FIDGET_PATTERN_WINDOW_SIZE) {
                fidgetPatternWindow.removeAt(0)
            }

            // Detect repetitive movements (a key indicator of fidgeting)
            detectRepetitiveMovements()
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

            // Update metrics regularly
            calculateAndUpdateMetrics()
        }

        // Update last values for next calculation
        lastAcceleration = Triple(movingAverageX, movingAverageY, movingAverageZ)
    }

    private fun processGyroscopeData(event: SensorEvent) {
        val x = event.values[0] // rotation around x axis (pitch)
        val y = event.values[1] // rotation around y axis (roll)
        val z = event.values[2] // rotation around z axis (yaw)

        // Calculate magnitude of rotation
        val magnitude = sqrt(x * x + y * y + z * z)

        // Store rotation data for analysis
        if (magnitude > 0.05f) { // Only store significant rotations
            synchronized(rotationData) {
                rotationData.add(
                    MotionDataPoint(
                        timestamp = System.currentTimeMillis(),
                        x = x,
                        y = y,
                        z = z,
                        magnitude = magnitude
                    )
                )
            }
        }
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

    // Improved fidget detection by looking for repetitive movement patterns
    private fun detectRepetitiveMovements() {
        if (fidgetPatternWindow.size < 10) return

        val now = System.currentTimeMillis()
        val recentWindow = fidgetPatternWindow.filter { now - it.first < REPETITIVE_PATTERN_WINDOW }

        if (recentWindow.size < 5) return

        // Extract the vectors
        val vectors = recentWindow.map { it.second }

        // Look for similar movement patterns (back-and-forth movements)
        var repetitiveFound = false
        val recentVector = vectors.last()

        // Look for vectors pointing in similar directions
        val similarDirections = vectors.dropLast(1).filter { vector ->
            val similarityScore = calculateVectorSimilarity(vector, recentVector)
            similarityScore > SIMILAR_DIRECTION_THRESHOLD
        }

        if (similarDirections.isNotEmpty()) {
            // If we have vectors pointing in similar directions within our time window,
            // this likely indicates repetitive fidgeting behavior
            repetitiveFound = true

            // Check if it's in the opposite direction of our last repetitive direction
            if (lastRepetitiveDirection != Triple(0f, 0f, 0f)) {
                val oppositeCheck = calculateVectorSimilarity(
                    Triple(-lastRepetitiveDirection.first, -lastRepetitiveDirection.second, -lastRepetitiveDirection.third),
                    recentVector
                )

                if (oppositeCheck > SIMILAR_DIRECTION_THRESHOLD &&
                    now - lastTimeRepeatedDirection < 1000) { // Within 1 second
                    // This is a back-and-forth movement - very indicative of fidgeting
                    repetitiveMovementCount += 2
                }
            }

            lastRepetitiveDirection = recentVector
            lastTimeRepeatedDirection = now
        }

        // Update fidget pattern score based on repetitive movements
        if (repetitiveFound) {
            // Calculate fidget score based on repetitive movement count
            fidgetPatternScore = (repetitiveMovementCount * 2).coerceIn(0, 100)
        }
    }

    private fun calculateVectorSimilarity(v1: Triple<Float, Float, Float>, v2: Triple<Float, Float, Float>): Float {
        // Simplistic dot product of unit vectors to measure similarity of direction
        val mag1 = sqrt(v1.first * v1.first + v1.second * v1.second + v1.third * v1.third)
        val mag2 = sqrt(v2.first * v2.first + v2.second * v2.second + v2.third * v2.third)

        if (mag1 == 0f || mag2 == 0f) return 0f

        val dotProduct = v1.first * v2.first + v1.second * v2.second + v1.third * v2.third
        return abs(dotProduct / (mag1 * mag2)) // Absolute value since we care about axis alignment, not direction
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

            // Time-based analysis parameters
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            val durationMinutes = sessionDuration / 60000f

            // Minimum intervals between counting events (to avoid overcounting)
            val MIN_DIRECTION_CHANGE_INTERVAL = 200L
            var lastSuddenMovementTime = 0L
            val MIN_SUDDEN_MOVEMENT_INTERVAL = 400L

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
                    val now = point.timestamp
                    if (now - lastSuddenMovementTime > MIN_SUDDEN_MOVEMENT_INTERVAL &&
                        suddenMovements < MAX_SUDDEN_MOVEMENTS) {
                        suddenMovements++
                        lastSuddenMovementTime = now
                    }
                }

                // Track direction changes
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

            // Analyze gyroscope data for rotational movements (often important for fidgeting)
            val rotationalEnergy = if (rotationData.isNotEmpty()) {
                rotationData.sumOf { it.magnitude.toDouble() } / rotationData.size
            } else 0.0

            // Scale and combine fidgeting indicators
            // For true fidgeting, we want to prioritize:
            // 1. Repetitive movements (most important)
            // 2. Small, quick movements
            // 3. Direction changes

            // Base fidget score on our repetitive movement detection
            var combinedFidgetScore = fidgetPatternScore

            // Add influence from small movements
            val smallMovementsScore = (fidgetFrames * 100f / totalFrames.coerceAtLeast(1)) * 0.3f

            // Add influence from directional changes if they're frequent (per minute)
            val directionChangesPerMinute = (directionChanges / durationMinutes).coerceIn(0f, 100f)
            val directionChangeScore = (directionChangesPerMinute * 0.3f).toInt()

            // Add rotational influence (wrist/hand fidgeting)
            val rotationalScore = (rotationalEnergy * 25).toInt().coerceIn(0, 50)

            // Combined calculation
            combinedFidgetScore = ((combinedFidgetScore * 0.5f) +
                    (smallMovementsScore * 0.2f) +
                    (directionChangeScore * 0.2f) +
                    (rotationalScore * 0.1f)).toInt()

            // Ensure it stays within 0-100
            val fidgetingScore = combinedFidgetScore.coerceIn(0, 100)

            // Calculate hyperactivity by considering larger movements
            val generalMovementScore =
                (((mediumMovementFrames + largeMovementFrames * 1.5) * 100.0 / totalFrames) * 0.9f)
                    .toInt().coerceIn(0, 100)

            // Movement intensity - average magnitude
            val movementIntensity = (totalMagnitude / totalFrames) * 0.9f

            // Overall restlessness - focused on larger body movements, distinct from fidgeting
            val largeMovementFactor = (largeMovementFrames * 100f / totalFrames.coerceAtLeast(1)) * 0.5f
            val suddenMovementFactor = (suddenMovements * 100f / MAX_SUDDEN_MOVEMENTS.coerceAtLeast(1)) * 0.25f
            val directionChangeFactor = (directionChanges * 100f / MAX_DIRECTION_CHANGES.coerceAtLeast(1)) * 0.25f

            // Combined factors for restlessness score - normalize based on session duration
            val durationNormalizer = if (durationMinutes < 1f) 1f else sqrt(durationMinutes)
            val restlessness = ((largeMovementFactor + suddenMovementFactor + directionChangeFactor) * durationNormalizer * 0.95f)
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
        // Make a final calculation
        val metrics = analyzeMotion()
        println("Final motion metrics - Fidgeting: ${metrics.fidgetingScore}%, Restlessness: ${metrics.restlessness}%")
        return metrics
    }
}