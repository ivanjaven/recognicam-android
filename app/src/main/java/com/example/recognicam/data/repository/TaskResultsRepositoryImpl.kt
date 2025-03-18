package com.example.recognicam.data.repository

import com.example.recognicam.domain.entity.*
import com.example.recognicam.domain.repository.ResultsRepository

class TaskResultsRepositoryImpl : ResultsRepository {

    private var cptResult: CPTTaskResult? = null
    private var readingResult: ReadingTaskResult? = null
    private var goNoGoResult: GoNoGoTaskResult? = null
    private var workingMemoryResult: WorkingMemoryTaskResult? = null
    private var attentionShiftingResult: AttentionShiftingTaskResult? = null

    override fun saveCPTResult(result: CPTTaskResult) {
        cptResult = result
    }

    override fun saveReadingResult(result: ReadingTaskResult) {
        readingResult = result
    }

    override fun saveGoNoGoResult(result: GoNoGoTaskResult) {
        goNoGoResult = result
    }

    override fun saveWorkingMemoryResult(result: WorkingMemoryTaskResult) {
        workingMemoryResult = result
    }

    override fun saveAttentionShiftingResult(result: AttentionShiftingTaskResult) {
        attentionShiftingResult = result
    }

    override fun getCPTResult(): CPTTaskResult? = cptResult

    override fun getReadingResult(): ReadingTaskResult? = readingResult

    override fun getGoNoGoResult(): GoNoGoTaskResult? = goNoGoResult

    override fun getWorkingMemoryResult(): WorkingMemoryTaskResult? = workingMemoryResult

    override fun getAttentionShiftingResult(): AttentionShiftingTaskResult? = attentionShiftingResult

    override fun getOverallADHDProbability(): Int {
        val results = listOfNotNull(
            cptResult?.adhdProbabilityScore,
            readingResult?.adhdProbabilityScore,
            goNoGoResult?.adhdProbabilityScore,
            workingMemoryResult?.adhdProbabilityScore,
            attentionShiftingResult?.adhdProbabilityScore
        )

        return if (results.isNotEmpty()) {
            results.sum() / results.size
        } else {
            0
        }
    }

    override fun clearAllResults() {
        cptResult = null
        readingResult = null
        goNoGoResult = null
        workingMemoryResult = null
        attentionShiftingResult = null
    }
}