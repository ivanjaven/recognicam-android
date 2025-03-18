package com.example.recognicam.domain.repository

import com.example.recognicam.domain.entity.*

interface ResultsRepository {
    fun saveCPTResult(result: CPTTaskResult)
    fun saveReadingResult(result: ReadingTaskResult)
    fun saveGoNoGoResult(result: GoNoGoTaskResult)
    fun saveWorkingMemoryResult(result: WorkingMemoryTaskResult)
    fun saveAttentionShiftingResult(result: AttentionShiftingTaskResult)

    fun getCPTResult(): CPTTaskResult?
    fun getReadingResult(): ReadingTaskResult?
    fun getGoNoGoResult(): GoNoGoTaskResult?
    fun getWorkingMemoryResult(): WorkingMemoryTaskResult?
    fun getAttentionShiftingResult(): AttentionShiftingTaskResult?

    fun getOverallADHDProbability(): Int
    fun clearAllResults()
}