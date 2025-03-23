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
    val fidgetingScore: Int = 0,           // 0-100 scale, percentage of restlessness that is fidgeting
    val generalMovementScore: Int = 0,     // 0-100 scale
    val directionChanges: Int = 0,         // Count
    val suddenMovements: Int = 0,          // Count
    val movementIntensity: Float = 0f,     // Raw value
    val restlessness: Int = 0              // 0-100 scale, % of time with meaningful movement
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
    private val NOISE_THRESHOLD = 0.1f            // Increased to reduce noise when stationary
    private val MEANINGFUL_MOVEMENT_THRESHOLD = 0.15f  // Threshold for counting as restlessness (increased)
    private val FIDGET_THRESHOLD = 0.2f           // Threshold for potential fidgeting (increased)
    private val MEDIUM_MOVEMENT_THRESHOLD = 0.5f  // Medium movements (increased)
    private val LARGE_MOVEMENT_THRESHOLD = 0.9f   // Large movements (increased)
    private val SUDDEN_MOVEMENT_THRESHOLD = 1.5f  // Sudden/fast movements (increased)

    // Repetitive movement detection (fidgeting)
    private val REPETITIVE_PATTERN_WINDOW = 2000L // 2 second window to detect repetitive movements
    private val SIMILAR_DIRECTION_THRESHOLD = 0.8f // Higher threshold for similarity (more strict)
    private val DIRECTION_CHANGE_THRESHOLD = -0.6f // Only strong direction reversals count (more strict)

    // Maximum counts to prevent inflated values
    private val MAX_DIRECTION_CHANGES = 120
    private val MAX_SUDDEN_MOVEMENTS = 50

    // Increased window size for better noise filtering
    private val FILTER_WINDOW_SIZE = 8
    private val recentReadings = mutableListOf<Triple<Float, Float, Float>>()

    // Time tracking for restlessness calculation
    private var totalTrackingTime = 0L
    private var restlessMovementTime = 0L
    private var fidgetingMovementTime = 0L
    private var lastDataPointTime = 0L
    private var lastMeaningfulMovementTime = 0L
    private var lastFidgetDetectionTime = 0L
    private var inRestlessState = false
    private var inFidgetingState = false

    // Fidget pattern improved detection
    private val fidgetPatternWindow = mutableListOf<Pair<Long, Triple<Float, Float, Float>>>() // Timestamp and movement vector
    private var directionChangesCount = 0
    private var lastDirectionChangeTime = 0L
    private var fidgetingDetectedCount = 0  // Track how many fidgeting periods we detect
    private var consecutiveFidgetDetections = 0

    // Movement history for detecting patterns
    private val movementHistory = mutableListOf<Pair<Long, Triple<Float, Float, Float>>>() // Time and direction

    // Timestamp of session start
    private var sessionStartTime = 0L

    // Additional stationary detection
    private var stationaryCount = 0
    private val STATIONARY_THRESHOLD = 20  // Number of consecutive readings below threshold to consider stationary
    private var isDeviceStationary = false
    private var stationarySince = 0L

    // State flow for real-time updates
    private val _motionMetrics = MutableStateFlow(MotionMetrics())
    val motionMetrics: StateFlow<MotionMetrics> = _motionMetrics.asStateFlow()

    fun startTracking() {
        if (isTrackingActive) return

        motionData.clear()
        rotationData.clear()
        recentReadings.clear()
        fidgetPatternWindow.clear()
        movementHistory.clear()
        directionChangesCount = 0
        fidgetingDetectedCount = 0
        consecutiveFidgetDetections = 0
        stationaryCount = 0
        isDeviceStationary = false
        stationarySince = 0L
        isTrackingActive = true
        lastAcceleration = Triple(0f, 0f, 0f)
        movingAverageX = 0f
        movingAverageY = 0f
        movingAverageZ = 0f
        sessionStartTime = System.currentTimeMillis()
        lastDataPointTime = sessionStartTime
        lastMeaningfulMovementTime = sessionStartTime
        lastFidgetDetectionTime = 0L
        totalTrackingTime = 0L
        restlessMovementTime = 0L
        fidgetingMovementTime = 0L
        inRestlessState = false
        inFidgetingState = false

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
        movementHistory.clear()
        directionChangesCount = 0
        fidgetingDetectedCount = 0
        consecutiveFidgetDetections = 0
        stationaryCount = 0
        isDeviceStationary = false
        stationarySince = 0L
        lastAcceleration = Triple(0f, 0f, 0f)
        movingAverageX = 0f
        movingAverageY = 0f
        movingAverageZ = 0f
        sessionStartTime = System.currentTimeMillis()
        lastDataPointTime = sessionStartTime
        lastMeaningfulMovementTime = sessionStartTime
        lastFidgetDetectionTime = 0L
        totalTrackingTime = 0L
        restlessMovementTime = 0L
        fidgetingMovementTime = 0L
        inRestlessState = false
        inFidgetingState = false
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
        val currentTime = System.currentTimeMillis()

        // Update total tracking time
        if (lastDataPointTime > 0) {
            val timeDelta = currentTime - lastDataPointTime
            totalTrackingTime += timeDelta
        }
        lastDataPointTime = currentTime

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

        // Improved stationary device detection
        if (magnitude < NOISE_THRESHOLD) {
            stationaryCount++
            if (stationaryCount >= STATIONARY_THRESHOLD && !isDeviceStationary) {
                // Device has become stationary
                isDeviceStationary = true
                stationarySince = currentTime
                println("Device detected as stationary")
            }
        } else {
            // Reset stationary counter if we detected movement
            if (stationaryCount > 0) {
                stationaryCount = 0
            }

            // If device was stationary and now moving, update status
            if (isDeviceStationary && magnitude > NOISE_THRESHOLD * 1.5) {
                isDeviceStationary = false
                println("Device no longer stationary")
            }
        }

        // If device is stationary, don't process for fidgeting or restlessness
        if (isDeviceStationary) {
            // Still update last values even when stationary
            lastAcceleration = Triple(movingAverageX, movingAverageY, movingAverageZ)

            // If stationary for more than 5 seconds, make sure all movement metrics are low/zero
            if (currentTime - stationarySince > 5000) {
                // Only update occasionally to prevent excessive processing
                if (currentTime % 1000 < 100) { // Update about every second
                    _motionMetrics.value = MotionMetrics(
                        fidgetingScore = 0,
                        generalMovementScore = 0,
                        directionChanges = directionChangesCount,
                        suddenMovements = 0,
                        movementIntensity = 0f,
                        restlessness = 0
                    )
                }
            }
            return
        }

        // Process movement data - only if above noise threshold and device not stationary
        if (magnitude > NOISE_THRESHOLD) {
            // Store direction for pattern analysis
            val direction = Triple(diffX, diffY, diffZ)

            // Add to movement history
            movementHistory.add(Pair(currentTime, direction))
            if (movementHistory.size > 30) { // Keep 30 most recent movements
                movementHistory.removeAt(0)
            }

            // Add to pattern window with direction vector
            fidgetPatternWindow.add(Pair(
                currentTime,
                direction
            ))

            // Keep the window size limited
            if (fidgetPatternWindow.size > 50) { // Reduced size for more recent focus
                fidgetPatternWindow.removeAt(0)
            }

            // Update restlessness tracking
            if (magnitude > MEANINGFUL_MOVEMENT_THRESHOLD) {
                // Calculate the time since last meaningful movement
                val timeDelta = currentTime - lastMeaningfulMovementTime

                // We have meaningful movement - transition to restless state if not already
                if (!inRestlessState) {
                    inRestlessState = true
                } else {
                    // Continue in restless state and update time
                    restlessMovementTime += timeDelta

                    // If currently in fidgeting state, update that time too
                    if (inFidgetingState) {
                        fidgetingMovementTime += timeDelta
                    }
                }

                // Update last meaningful movement time
                lastMeaningfulMovementTime = currentTime

                // Only check for fidgeting if magnitude is in the right range
                // Too small might be normal hand movement, too large is deliberate movement not fidgeting
                if (magnitude > FIDGET_THRESHOLD && magnitude < LARGE_MOVEMENT_THRESHOLD) {
                    // Only check for fidgeting periodically to avoid over-detection
                    if (currentTime - lastFidgetDetectionTime > 300) { // Check every 300ms
                        val isFidgeting = detectFidgeting(currentTime, direction, magnitude)

                        if (isFidgeting) {
                            // Start or continue fidgeting state
                            if (!inFidgetingState) {
                                inFidgetingState = true
                                fidgetingDetectedCount++
                                consecutiveFidgetDetections++
                            }
                        } else {
                            // Exit fidgeting state after a short period with no detection
                            if (inFidgetingState && currentTime - lastFidgetDetectionTime > 1000) {
                                inFidgetingState = false
                                consecutiveFidgetDetections = 0
                            }
                        }

                        lastFidgetDetectionTime = currentTime
                    }
                } else {
                    // Exit fidgeting state if movement is outside the fidgeting range
                    if (inFidgetingState && currentTime - lastFidgetDetectionTime > 800) {
                        inFidgetingState = false
                        consecutiveFidgetDetections = 0
                    }
                }
            } else {
                // Exit restless state after a short delay without meaningful movement
                if (inRestlessState && (currentTime - lastMeaningfulMovementTime > 500)) {
                    inRestlessState = false
                    inFidgetingState = false
                    consecutiveFidgetDetections = 0
                }
            }

            // Check for direction changes - useful for general metrics
            if (movementHistory.size >= 2) {
                val prev = movementHistory[movementHistory.size - 2].second
                val curr = direction

                // Check if direction changed significantly
                val similarity = calculateVectorSimilarity(prev, curr)
                if (magnitude > MEANINGFUL_MOVEMENT_THRESHOLD &&
                    similarity < DIRECTION_CHANGE_THRESHOLD && // More strict direction reversal
                    currentTime - lastDirectionChangeTime > 200) { // Not too frequent

                    directionChangesCount = (directionChangesCount + 1).coerceAtMost(MAX_DIRECTION_CHANGES)
                    lastDirectionChangeTime = currentTime
                }
            }

            // Store data
            synchronized(motionData) {
                motionData.add(
                    MotionDataPoint(
                        timestamp = currentTime,
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

    private fun detectFidgeting(currentTime: Long, direction: Triple<Float, Float, Float>, magnitude: Float): Boolean {
        // Stricter fidgeting detection: require multiple pieces of evidence
        var evidence = 0

        // 1. Check for frequent direction changes in short window (classic fidgeting behavior)
        val recentWindow = 1500L // 1.5 seconds
        val recentDirectionChanges = countDirectionChangesInWindow(currentTime - recentWindow, currentTime)
        if (recentDirectionChanges >= 3) evidence++

        // 2. Check for repeating patterns (same movement repeated multiple times)
        val hasRepeatingPattern = detectRepeatingPattern(currentTime, direction)
        if (hasRepeatingPattern) evidence++

        // 3. Check for back-and-forth movements (alternating opposite directions)
        val hasBackAndForthPattern = detectBackAndForthPattern(currentTime)
        if (hasBackAndForthPattern) evidence++

        // Require at least 2 pieces of evidence for true fidgeting
        return evidence >= 2
    }

    private fun countDirectionChangesInWindow(startTime: Long, endTime: Long): Int {
        // Count significant direction changes within specified time window
        var count = 0
        var lastChangeTime = 0L

        for (i in 1 until movementHistory.size) {
            val time = movementHistory[i].first
            if (time < startTime || time > endTime) continue

            val prev = movementHistory[i-1].second
            val curr = movementHistory[i].second

            val similarity = calculateVectorSimilarity(prev, curr)
            // Only count strong direction reversals (dot product significantly negative)
            if (similarity < DIRECTION_CHANGE_THRESHOLD && time - lastChangeTime > 200) {
                count++
                lastChangeTime = time
            }
        }

        return count
    }

    private fun detectRepeatingPattern(currentTime: Long, currentDirection: Triple<Float, Float, Float>): Boolean {
        // Look for patterns where movement repeats in a similar direction
        val recentMovements = fidgetPatternWindow
            .filter { currentTime - it.first < REPETITIVE_PATTERN_WINDOW }

        if (recentMovements.size < 5) return false // Need enough data

        // Check for similar movements spaced out in time (repetition)
        var similarPairsCount = 0
        for (i in 0 until recentMovements.size - 3) {
            for (j in i + 3 until recentMovements.size) { // Skip adjacent movements - need time gap
                val similarity = calculateVectorSimilarity(
                    recentMovements[i].second,
                    recentMovements[j].second
                )
                // Only count very similar movements
                if (similarity > SIMILAR_DIRECTION_THRESHOLD) {
                    similarPairsCount++
                    if (similarPairsCount >= 3) return true // Need multiple repeats
                }
            }
        }

        return false
    }

    private fun detectBackAndForthPattern(currentTime: Long): Boolean {
        // Look for classic back and forth fidgeting
        val recentMovements = fidgetPatternWindow
            .filter { currentTime - it.first < REPETITIVE_PATTERN_WINDOW }
            .map { it.second }

        if (recentMovements.size < 6) return false // Need enough movements

        // Look for alternating directions (-1, 1, -1, 1, -1, 1)
        var alternatingCount = 0
        for (i in 2 until recentMovements.size) {
            val v1 = recentMovements[i-2]
            val v2 = recentMovements[i]

            // Check if they're pointing in the same direction
            val similarity = calculateVectorSimilarity(v1, v2)

            // Similar movements separated by another movement
            if (similarity > SIMILAR_DIRECTION_THRESHOLD) {
                alternatingCount++
                if (alternatingCount >= 2) return true // Found enough alternating pattern
            }
        }

        return false
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

    private fun calculateVectorSimilarity(v1: Triple<Float, Float, Float>, v2: Triple<Float, Float, Float>): Float {
        // Calculate dot product of normalized vectors
        val mag1 = sqrt(v1.first * v1.first + v1.second * v1.second + v1.third * v1.third)
        val mag2 = sqrt(v2.first * v2.first + v2.second * v2.second + v2.third * v2.third)

        if (mag1 < 0.001f || mag2 < 0.001f) return 0f

        // Normalize vectors
        val n1 = Triple(v1.first/mag1, v1.second/mag1, v1.third/mag1)
        val n2 = Triple(v2.first/mag2, v2.second/mag2, v2.third/mag2)

        // Dot product (-1 to 1)
        // 1: same direction, 0: perpendicular, -1: opposite direction
        return n1.first * n2.first + n1.second * n2.second + n1.third * n2.third
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

        // If device is detected as stationary for a significant time, return zero values
        if (isDeviceStationary && System.currentTimeMillis() - stationarySince > 3000) {
            return MotionMetrics(
                fidgetingScore = 0,
                generalMovementScore = 0,
                directionChanges = directionChangesCount,
                suddenMovements = 0,
                movementIntensity = 0f,
                restlessness = 0
            )
        }

        synchronized(motionData) {
            val totalFrames = motionData.size
            var mediumMovementFrames = 0
            var largeMovementFrames = 0
            var suddenMovements = 0
            var totalMagnitude = 0f
            var prevDirection = Triple(0, 0, 0)
            var lastSuddenMovementTime = 0L
            val MIN_SUDDEN_MOVEMENT_INTERVAL = 400L

            // Analyze each data point for additional metrics
            for (point in motionData) {
                // Classify movement intensity
                when {
                    point.magnitude >= LARGE_MOVEMENT_THRESHOLD -> largeMovementFrames++
                    point.magnitude >= MEDIUM_MOVEMENT_THRESHOLD -> mediumMovementFrames++
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

                // Track direction changes (already tracked in main processing)

                totalMagnitude += point.magnitude
            }

            // Analyze gyroscope data for rotational movements
            val rotationalEnergy = if (rotationData.isNotEmpty()) {
                rotationData.sumOf { it.magnitude.toDouble() } / rotationData.size
            } else 0.0

            // Calculate general movement score
            val generalMovementScore = ((mediumMovementFrames + largeMovementFrames * 1.5) * 100.0 / totalFrames * 0.9f)
                .toInt().coerceIn(0, 100)

            // Movement intensity - average magnitude
            val movementIntensity = if (totalFrames > 0) (totalMagnitude / totalFrames) * 0.9f else 0f

            // Calculate restlessness as percentage of total time
            val restlessnessPercentage = if (totalTrackingTime > 0) {
                ((restlessMovementTime * 100f) / totalTrackingTime).toInt().coerceIn(0, 100)
            } else {
                0
            }

            // Calculate fidgeting as percentage of restlessness - without artificial cap
            val fidgetingPercentage = if (restlessMovementTime > 0) {
                ((fidgetingMovementTime * 100f) / restlessMovementTime).toInt().coerceIn(0, 100)
            } else {
                0
            }

            // Debug output
            println("Motion analysis: Tracking=${totalTrackingTime}ms, " +
                    "Restless=${restlessMovementTime}ms (${restlessnessPercentage}%), " +
                    "Fidget=${fidgetingMovementTime}ms (${fidgetingPercentage}% of restlessness)")

            return MotionMetrics(
                fidgetingScore = fidgetingPercentage,
                generalMovementScore = generalMovementScore,
                directionChanges = directionChangesCount,
                suddenMovements = suddenMovements,
                movementIntensity = movementIntensity,
                restlessness = restlessnessPercentage
            )
        }
    }

    fun getFinalMetrics(): MotionMetrics {
        // Make a final calculation
        val metrics = analyzeMotion()
        println("Final motion metrics - Restlessness: ${metrics.restlessness}% of time, " +
                "Fidgeting: ${metrics.fidgetingScore}% of restlessness time")
        return metrics
    }
}