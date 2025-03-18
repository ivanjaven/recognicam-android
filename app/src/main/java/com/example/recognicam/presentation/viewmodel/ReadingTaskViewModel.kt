// ReadingTaskViewModel.kt
package com.example.recognicam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.recognicam.domain.entity.ReadingTaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReadingTaskViewModel : ViewModel() {

    // Task UI state
    private val _uiState = MutableStateFlow<ReadingTaskState>(ReadingTaskState.Instructions)
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

    // Passage and questions
    private val passage = ReadingPassage(
        title = "The Human Brain",
        content = """
            The human brain is the central organ of the human nervous system. Along with the spinal cord, it makes up the central nervous system. The brain consists of the cerebrum, the brainstem and the cerebellum. It controls most of the activities of the body, processing, integrating, and coordinating the information it receives from the sense organs, and making decisions as to the instructions sent to the rest of the body.
            
            The cerebrum is the largest part of the human brain. It is divided into two cerebral hemispheres. The cerebral cortex is an outer layer of grey matter, covering the core of white matter. The cortex is split into the neocortex and the much smaller allocortex. The neocortex is made up of six neuronal layers, while the allocortex has three or four.
            
            Each hemisphere is conventionally divided into four lobes – the frontal, temporal, parietal, and occipital lobes. The frontal lobe is associated with executive functions including self-control, planning, reasoning, and abstract thought, while the occipital lobe is dedicated to vision. Within each lobe, cortical areas are associated with specific functions, such as the sensory, motor and association regions.
            
            The cerebellum is divided into an anterior lobe, a posterior lobe, and the flocculonodular lobe. The anterior and posterior lobes are connected in the middle by the vermis. Compared to the cerebral cortex, the cerebellum has a much thinner outer cortex. The cerebellum's anterior and posterior lobes appear to play a role in the coordination and smoothing of complex motor movements, and the flocculonodular lobe in the maintenance of balance.
        """.trimIndent(),
        questions = listOf(
            Question(
                id = 1,
                text = "What are the three main parts of the human brain?",
                options = listOf(
                    "Cerebrum, brainstem, and occipital lobe",
                    "Cerebrum, brainstem, and cerebellum",
                    "Neocortex, allocortex, and cerebellum",
                    "Frontal lobe, temporal lobe, and parietal lobe"
                ),
                correctAnswerIndex = 1
            ),
            Question(
                id = 2,
                text = "Which lobe of the brain is associated with vision?",
                options = listOf(
                    "Frontal lobe",
                    "Temporal lobe",
                    "Parietal lobe",
                    "Occipital lobe"
                ),
                correctAnswerIndex = 3
            ),
            Question(
                id = 3,
                text = "What is the function of the cerebellum?",
                options = listOf(
                    "Processing visual information",
                    "Controlling executive functions",
                    "Coordinating complex motor movements and balance",
                    "Processing auditory information"
                ),
                correctAnswerIndex = 2
            )
        )
    )

    fun startCountdown() {
        _uiState.value = ReadingTaskState.Countdown(3)
    }

    fun startReading() {
        readingStartTime = System.currentTimeMillis()
        _uiState.value = ReadingTaskState.Reading(passage)
    }

    fun finishReading() {
        readingEndTime = System.currentTimeMillis()
        _currentQuestionIndex.value = 0
        _selectedOptionIndex.value = null
        _uiState.value = ReadingTaskState.Questions(
            question = passage.questions[0],
            currentIndex = 0,
            totalQuestions = passage.questions.size
        )
    }

    fun selectAnswer(optionIndex: Int) {
        val currentQuestion = passage.questions[_currentQuestionIndex.value]
        _selectedOptionIndex.value = optionIndex

        // Record if answer is correct
        if (optionIndex == currentQuestion.correctAnswerIndex) {
            correctAnswers++
        } else {
            incorrectAnswers++
        }
    }

    fun nextQuestion() {
        val nextIndex = _currentQuestionIndex.value + 1
        _selectedOptionIndex.value = null

        if (nextIndex < passage.questions.size) {
            _currentQuestionIndex.value = nextIndex
            _uiState.value = ReadingTaskState.Questions(
                question = passage.questions[nextIndex],
                currentIndex = nextIndex,
                totalQuestions = passage.questions.size
            )
        } else {
            completeTask()
        }
    }

    private fun completeTask() {
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

        // ADHD probability calculation
        // Lower reading speed and comprehension correlate with ADHD symptoms
        val speedFactor = if (readingSpeed < 150) 40 else
            if (readingSpeed < 200) 30 else
                if (readingSpeed < 250) 20 else 10

        val comprehensionFactor = if (comprehensionScore < 50) 40 else
            if (comprehensionScore < 70) 30 else
                if (comprehensionScore < 85) 20 else 10

        val readingTimeFactor = if (readingTimeSeconds > 120) 20 else
            if (readingTimeSeconds > 90) 15 else
                if (readingTimeSeconds > 60) 10 else 5

        val adhdProbabilityScore = (speedFactor + comprehensionFactor + readingTimeFactor)
            .coerceIn(0, 100)

        val result = ReadingTaskResult(
            readingTime = readingTimeSeconds,
            correctAnswers = correctAnswers,
            incorrectAnswers = incorrectAnswers,
            readingSpeed = readingSpeed,
            comprehensionScore = comprehensionScore,
            adhdProbabilityScore = adhdProbabilityScore
        )

        _uiState.value = ReadingTaskState.Completed(result)
    }

    fun onCountdownComplete() {
        startReading()
    }
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

sealed class ReadingTaskState {
    object Instructions : ReadingTaskState()
    data class Countdown(val count: Int) : ReadingTaskState()
    data class Reading(val passage: ReadingPassage) : ReadingTaskState()
    data class Questions(
        val question: Question,
        val currentIndex: Int,
        val totalQuestions: Int
    ) : ReadingTaskState()
    data class Completed(val result: ReadingTaskResult) : ReadingTaskState()
}