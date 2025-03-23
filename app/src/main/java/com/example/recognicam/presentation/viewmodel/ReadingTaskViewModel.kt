package com.example.recognicam.presentation.viewmodel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.base.BaseAssessmentTaskViewModel
import com.example.recognicam.data.analysis.ADHDAnalyzer
import com.example.recognicam.data.analysis.ADHDAssessmentResult
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import com.example.recognicam.domain.entity.ReadingTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Definition of UI state for Reading task
sealed class ReadingTaskState {
    object PreInstructions : ReadingTaskState()
    object Instructions : ReadingTaskState()
    data class Countdown(val count: Int) : ReadingTaskState()
    data class Reading(val passage: ReadingPassage) : ReadingTaskState()
    data class Questions(
        val question: Question,
        val currentIndex: Int,
        val totalQuestions: Int
    ) : ReadingTaskState()
    data class Completed(val result: ReadingTaskResultUI) : ReadingTaskState()
}

data class ReadingPassage(
    val title: String,
    val content: String,
    val questions: List<Question>
)

data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

// UI model for results (matching CPT format)
data class ReadingTaskResultUI(
    val readingTime: Int, // in seconds
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val readingSpeed: Int, // words per minute
    val comprehensionScore: Int,
    val faceMetrics: FaceMetrics,
    val motionMetrics: MotionMetrics,
    val adhdAssessment: ADHDAssessmentResult
)

class ReadingTaskViewModel(
    private val context: Context,
    private val isPartOfFullAssessment: Boolean = false
) : BaseAssessmentTaskViewModel() {

    private val adhdAnalyzer = ADHDAnalyzer()

    // Task UI state
    private val _uiState = MutableStateFlow<ReadingTaskState>(
        if (isPartOfFullAssessment) ReadingTaskState.Instructions
        else ReadingTaskState.PreInstructions
    )
    val uiState: StateFlow<ReadingTaskState> = _uiState.asStateFlow()

    // Current question management
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()

    // Performance metrics
    private var readingStartTime = 0L
    private var readingEndTime = 0L
    private var correctAnswers = 0
    private var incorrectAnswers = 0
    private var selectedAnswers = mutableMapOf<Int, Int>() // Question ID to selected answer index

    // Child-friendly passage
    private val passage = ReadingPassage(
        title = "The Amazing Animal Friends",
        content = """
            Once upon a time, there was a small forest where all the animals lived together as friends. The clever fox, the wise owl, and the playful rabbit were the best of friends. They would play games, find food, and help each other every day.

            One sunny morning, the fox found a shiny round object near the river. It sparkled in the sunlight. "What could this be?" thought the fox. He decided to show it to his friends.

            First, he went to the owl who lived in the tallest tree. "Owl, look what I found by the river!" said the fox. The owl adjusted her glasses and looked carefully. "That's a special stone that fell from the sky last night. It's called a star stone," said the owl.

            Next, they both went to the rabbit's burrow. "Rabbit, come see what I found!" called the fox. The rabbit hopped out and stared at the stone with wide eyes. "It's so pretty!" she said, touching it gently.

            The three friends decided to place the star stone in the center of the forest where all animals could enjoy its beauty. They dug a small hole and placed it carefully.

            That night, something magical happened. The stone began to glow with a soft blue light! It lit up the entire forest, making it easier for all the nocturnal animals to see. The hedgehogs could find more bugs, the mice could gather more seeds, and the bats could fly without bumping into trees.

            From that day on, the forest became known as the "Glowing Forest," and animals from far and wide would come to visit and see the magical star stone that the three friends had found.

            The fox, owl, and rabbit were very proud of themselves for sharing their discovery with everyone instead of keeping it for themselves. They learned that sharing something special with others can bring joy to many.
        """.trimIndent(),
        questions = listOf(
            Question(
                id = 1,
                text = "Who were the three best friends in the forest?",
                options = listOf(
                    "Fox, bear, and rabbit",
                    "Fox, owl, and rabbit",
                    "Owl, rabbit, and squirrel",
                    "Fox, owl, and hedgehog"
                ),
                correctAnswerIndex = 1
            ),
            Question(
                id = 2,
                text = "What did the fox find near the river?",
                options = listOf(
                    "A fish",
                    "A bird",
                    "A star stone",
                    "A toy"
                ),
                correctAnswerIndex = 2
            ),
            Question(
                id = 3,
                text = "What happened to the stone at night?",
                options = listOf(
                    "It disappeared",
                    "It turned into a star",
                    "It began to glow blue",
                    "It grew bigger"
                ),
                correctAnswerIndex = 2
            ),
            Question(
                id = 4,
                text = "What did the friends learn in the story?",
                options = listOf(
                    "Never trust strangers",
                    "Always keep treasures hidden",
                    "Sharing brings joy to many",
                    "Forests are dangerous at night"
                ),
                correctAnswerIndex = 2
            ),
            Question(
                id = 5,
                text = "What was the forest called after they found the stone?",
                options = listOf(
                    "Starry Forest",
                    "Blue Forest",
                    "Glowing Forest",
                    "Magic Forest"
                ),
                correctAnswerIndex = 2
            )
        )
    )

    init {
        // Just initialize the services without starting them
        if (!motionDetectionService.isTracking()) {
            motionDetectionService.resetTracking()
        }
    }

    fun proceedToTaskInstructions() {
        _uiState.value = ReadingTaskState.Instructions
    }

    fun startCountdown() {
        _uiState.value = ReadingTaskState.Countdown(3)

        super.startCountdown(3) {
            startReading()
        }
    }

    private fun startReading() {
        // Start sensors
        startSensors()

        // Record start time
        readingStartTime = System.currentTimeMillis()

        // Start sensor update timer
        startSensorUpdateTimer()

        // Reset metrics
        correctAnswers = 0
        incorrectAnswers = 0
        selectedAnswers.clear()

        // Show reading passage
        _uiState.value = ReadingTaskState.Reading(passage)

        // DO NOT call startMainTimer() here as it would trigger automatic completion
    }

    fun finishReading() {
        // Record end time
        readingEndTime = System.currentTimeMillis()

        // Reset question index and selection
        _currentQuestionIndex.value = 0
        _selectedOptionIndex.value = null

        // Move to questions
        _uiState.value = ReadingTaskState.Questions(
            question = passage.questions[0],
            currentIndex = 0,
            totalQuestions = passage.questions.size
        )
    }

    fun selectAnswer(optionIndex: Int) {
        val currentQuestion = passage.questions[_currentQuestionIndex.value]
        _selectedOptionIndex.value = optionIndex

        // Store the selected answer (can be changed later)
        selectedAnswers[currentQuestion.id] = optionIndex
    }

    fun nextQuestion() {
        val nextIndex = _currentQuestionIndex.value + 1
        _selectedOptionIndex.value = null

        if (nextIndex < passage.questions.size) {
            _currentQuestionIndex.value = nextIndex

            // Restore previous answer if it exists
            val nextQuestion = passage.questions[nextIndex]
            _selectedOptionIndex.value = selectedAnswers[nextQuestion.id]

            _uiState.value = ReadingTaskState.Questions(
                question = nextQuestion,
                currentIndex = nextIndex,
                totalQuestions = passage.questions.size
            )
        } else {
            // All questions answered, calculate results
            calculateResults()
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            val prevIndex = _currentQuestionIndex.value - 1
            _currentQuestionIndex.value = prevIndex

            // Restore previous answer if it exists
            val prevQuestion = passage.questions[prevIndex]
            _selectedOptionIndex.value = selectedAnswers[prevQuestion.id]

            _uiState.value = ReadingTaskState.Questions(
                question = prevQuestion,
                currentIndex = prevIndex,
                totalQuestions = passage.questions.size
            )
        }
    }

    private fun calculateResults() {
        // Calculate correct/incorrect answers
        correctAnswers = 0
        incorrectAnswers = 0

        passage.questions.forEach { question ->
            val selectedOption = selectedAnswers[question.id]
            if (selectedOption != null) {
                if (selectedOption == question.correctAnswerIndex) {
                    correctAnswers++
                } else {
                    incorrectAnswers++
                }
            } else {
                // Count unanswered as incorrect
                incorrectAnswers++
            }
        }

        completeTask()
    }

    override fun startTask() {
        // This is handled differently for reading task - called by startReading()
    }

    override fun completeTask() {
        // Clean up timers
        mainTimer?.cancel()
        sensorUpdateTimer?.cancel()

        // One final update of sensor metrics
        updateSensorMetrics()

        // Get final sensor metrics
        val finalFaceMetrics = _faceMetrics.value
        val finalMotionMetrics = _motionMetrics.value

        // Calculate reading time in seconds
        val readingTimeSeconds = ((readingEndTime - readingStartTime) / 1000).toInt()

        // Calculate words per minute
        val wordCount = passage.content.split(Regex("\\s+")).size
        val readingSpeed = if (readingTimeSeconds > 0) {
            (wordCount * 60) / readingTimeSeconds
        } else {
            0
        }

        // Calculate comprehension score
        val totalQuestions = passage.questions.size
        val comprehensionScore = if (totalQuestions > 0) {
            (correctAnswers * 100) / totalQuestions
        } else {
            0
        }

        // Stop tracking
        if (motionDetectionService.isTracking()) {
            motionDetectionService.stopTracking()
        }

        // Analyze ADHD indicators
        val adhdAssessment = adhdAnalyzer.analyzePerformance(
            correctResponses = correctAnswers,
            incorrectResponses = incorrectAnswers,
            missedResponses = 0, // Not applicable for reading
            averageResponseTime = readingTimeSeconds * 1000 / totalQuestions, // Approximate time per question
            responseTimeVariability = 0f, // Not tracked in reading task
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            durationSeconds = readingTimeSeconds
        )

        // Create domain entity for storage in repository
        val domainResult = ReadingTaskResult(
            readingTime = readingTimeSeconds,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
            readingSpeed = readingSpeed,
            comprehensionScore = comprehensionScore,
            adhdProbabilityScore = adhdAssessment.adhdProbabilityScore
        )

        // Save to repository
        resultsRepository.saveReadingResult(domainResult)

        // Create presentation model for UI (matching CPT format)
        val uiResult = ReadingTaskResultUI(
            readingTime = readingTimeSeconds,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
            readingSpeed = readingSpeed,
            comprehensionScore = comprehensionScore,
            faceMetrics = finalFaceMetrics,
            motionMetrics = finalMotionMetrics,
            adhdAssessment = adhdAssessment
        )

        // Update UI state with the presentation model
        _uiState.value = ReadingTaskState.Completed(uiResult)
    }

    fun processFaceImage(imageProxy: androidx.camera.core.ImageProxy) {
        if (_uiState.value is ReadingTaskState.Reading || _uiState.value is ReadingTaskState.Questions) {
            faceAnalysisService.processImage(imageProxy)
        } else {
            imageProxy.close()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val isPartOfFullAssessment: Boolean = false  // Add this parameter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReadingTaskViewModel::class.java)) {
                return ReadingTaskViewModel(context, isPartOfFullAssessment) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}