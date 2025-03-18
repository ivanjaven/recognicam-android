package com.example.recognicam.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recognicam.core.ServiceLocator
import com.example.recognicam.domain.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ResultsViewModel(private val taskType: String) : ViewModel() {

    private val resultsRepository = ServiceLocator.getResultsRepository()

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
    }

    private fun loadResults() {
        val result = when (taskType) {
            "CPT" -> resultsRepository.getCPTResult()
            "Reading" -> resultsRepository.getReadingResult()
            "GoNoGo" -> resultsRepository.getGoNoGoResult()
            "WorkingMemory" -> resultsRepository.getWorkingMemoryResult()
            "AttentionShifting" -> resultsRepository.getAttentionShiftingResult()
            else -> null
        }

        if (result != null) {
            _uiState.value = ResultsUiState.Success(taskType, result)
        } else {
            // In the demo, use sample data
            _uiState.value = ResultsUiState.Success(
                taskType = taskType,
                result = createSampleResult(taskType)
            )
        }
    }

    private fun createSampleResult(taskType: String): Any {
        // Generate sample results for demonstration purposes
        return when (taskType) {
            "CPT" -> CPTTaskResult(
                correctResponses = 24,
                incorrectResponses = 3,
                missedResponses = 4,
                accuracy = 78,
                averageResponseTime = 320,
                fidgetingScore = 36,
                generalMovementScore = 42,
                directionChanges = 15,
                adhdProbabilityScore = 45
            )
            "Reading" -> ReadingTaskResult(
                readingTime = 95,
                correctAnswers = 2,
                incorrectAnswers = 1,
                readingSpeed = 180,
                comprehensionScore = 67,
                adhdProbabilityScore = 50
            )
            "GoNoGo" -> GoNoGoTaskResult(
                correctGo = 30,
                correctNoGo = 12,
                missedGo = 5,
                incorrectNoGo = 4,
                accuracy = 80,
                averageResponseTime = 420,
                adhdProbabilityScore = 40
            )
            "WorkingMemory" -> WorkingMemoryTaskResult(
                correctResponses = 18,
                incorrectResponses = 7,
                missedResponses = 5,
                accuracy = 60,
                averageResponseTime = 550,
                memorySpan = 1,
                adhdProbabilityScore = 55
            )
            "AttentionShifting" -> AttentionShiftingTaskResult(
                correctResponses = 22,
                incorrectResponses = 8,
                missedResponses = 6,
                accuracy = 61,
                averageResponseTime = 480,
                shiftingCost = 220,
                adhdProbabilityScore = 60
            )
            else -> CPTTaskResult(
                correctResponses = 0,
                incorrectResponses = 0,
                missedResponses = 0,
                accuracy = 0,
                averageResponseTime = 0,
                adhdProbabilityScore = 0
            )
        }
    }

    fun clearResults() {
        resultsRepository.clearAllResults()
    }

    // Custom factory for passing parameters to ViewModel
    class Factory(private val taskType: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResultsViewModel::class.java)) {
                return ResultsViewModel(taskType) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class ResultsUiState {
    object Loading : ResultsUiState()
    data class Success(val taskType: String, val result: Any) : ResultsUiState()
    data class Error(val message: String) : ResultsUiState()
}