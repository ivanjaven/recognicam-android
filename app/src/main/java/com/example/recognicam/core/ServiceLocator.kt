package com.example.recognicam.core

import android.content.Context
import com.example.recognicam.data.repository.TaskResultsRepositoryImpl
import com.example.recognicam.data.sensor.MotionDetectionService
import com.example.recognicam.data.sensor.FaceAnalysisService
import com.example.recognicam.domain.repository.ResultsRepository

object ServiceLocator {
    private var applicationContext: Context? = null

    // Singletons
    private var motionDetectionService: MotionDetectionService? = null
    private var faceDetectionService: FaceAnalysisService? = null
    private var resultsRepository: ResultsRepository? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun getMotionDetectionService(): MotionDetectionService {
        return motionDetectionService ?: synchronized(this) {
            motionDetectionService ?: MotionDetectionService(
                requireNotNull(applicationContext) { "ServiceLocator must be initialized first" }
            ).also { motionDetectionService = it }
        }
    }

    fun getFaceDetectionService(): FaceAnalysisService {
        return faceDetectionService ?: synchronized(this) {
            faceDetectionService ?: FaceAnalysisService().also { faceDetectionService = it }
        }
    }

    fun getResultsRepository(): ResultsRepository {
        return resultsRepository ?: synchronized(this) {
            resultsRepository ?: TaskResultsRepositoryImpl().also { resultsRepository = it }
        }
    }
}