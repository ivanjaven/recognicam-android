// BaseTaskState.kt
package com.example.recognicam.core.base

sealed class BaseTaskState {
    object Instructions : BaseTaskState()
    data class Countdown(val count: Int) : BaseTaskState()
    object Running : BaseTaskState()
    // Completed states will be implemented by specific tasks
}