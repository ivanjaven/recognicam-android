// GoNoGoTaskScreen.kt
package com.example.recognicam.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.domain.entity.GoNoGoTaskResult
import com.example.recognicam.presentation.components.CountdownTimer
import com.example.recognicam.presentation.components.InstructionBar
import com.example.recognicam.presentation.components.TaskInstructions
import com.example.recognicam.presentation.viewmodel.GoNoGoTaskState
import com.example.recognicam.presentation.viewmodel.GoNoGoTaskViewModel
import com.example.recognicam.presentation.viewmodel.StimulusType
import kotlinx.coroutines.delay

@Composable
fun GoNoGoTaskScreen(
    navController: NavController,
    viewModel: GoNoGoTaskViewModel = viewModel()
) {
    // Configure task for shorter duration in demo
    LaunchedEffect(Unit) {
        viewModel.setTaskDuration(40) // 40 seconds for regular testing
    }

    val uiState by viewModel.uiState.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val stimulusType by viewModel.stimulusType.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()

    when (val state = uiState) {
        is GoNoGoTaskState.Instructions -> {
            TaskInstructions(
                title = "Go/No-Go Task",
                instructions = listOf(
                    "In this task, you will see green and red circles appear on the screen.",
                    "TAP when you see a GREEN circle (GO).",
                    "DO NOT TAP when you see a RED circle (NO-GO).",
                    "Try to respond as quickly and accurately as possible."
                ),
                onButtonPress = { viewModel.startCountdown() }
            )
        }

        is GoNoGoTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is GoNoGoTaskState.Running -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // Timer display
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                )

                // Main content area for stimulus
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewModel.handleResponse() },
                    contentAlignment = Alignment.Center
                ) {
                    if (stimulusVisible && stimulusType != null) {
                        // Show colored circle based on stimulus type
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(
                                    when (stimulusType) {
                                        StimulusType.GO -> Color(0xFF4CAF50) // Green
                                        StimulusType.NO_GO -> Color(0xFFF44336) // Red
                                        null -> TODO()
                                    }
                                )
                        )
                    }
                }

                // Bottom instruction bar
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    InstructionBar(text = "Tap for Green • Don't Tap for Red")
                }
            }
        }

        is GoNoGoTaskState.Completed -> {
            // Navigate to results
            LaunchedEffect(state) {
                delay(100) // Small delay to ensure ViewModel state is stable
                navigateToResults(navController, state.result)
            }

            // Loading indicator while navigating
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

private fun navigateToResults(navController: NavController, result: GoNoGoTaskResult) {
    // In a real app, you'd pass this data through a repository or shared ViewModel
    // For now, we'll just navigate to the results screen
    navController.navigate("results/GoNoGo") {
        // Clear back stack so user can't go back to the task
        popUpTo("home")
    }
}