package com.example.recognicam.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.CountdownTimer
import com.example.recognicam.presentation.components.InstructionBar
import com.example.recognicam.presentation.components.TaskInstructions
import com.example.recognicam.presentation.viewmodel.AttentionShiftingTaskState
import com.example.recognicam.presentation.viewmodel.AttentionShiftingTaskViewModel
import com.example.recognicam.presentation.viewmodel.Rule
import kotlinx.coroutines.delay

@Composable
fun AttentionShiftingTaskScreen(
    navController: NavController,
    viewModel: AttentionShiftingTaskViewModel = viewModel()
) {
    // Configure task for regular testing
    LaunchedEffect(Unit) {
        viewModel.setTaskDuration(60) // 60 seconds for regular testing
    }

    val uiState by viewModel.uiState.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val currentShape by viewModel.currentShape.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()
    val currentRule by viewModel.currentRule.collectAsState()

    when (val state = uiState) {
        is AttentionShiftingTaskState.Instructions -> {
            TaskInstructions(
                title = "Attention Shifting Task",
                instructions = listOf(
                    "In this task, you'll need to tap the screen based on changing rules.",
                    "First rule: Tap when you see BLUE shapes.",
                    "Second rule: Tap when you see SQUARE shapes.",
                    "The rule will change periodically. Pay attention to the instruction at the bottom of the screen."
                ),
                onButtonPress = { viewModel.startCountdown() }
            )
        }

        is AttentionShiftingTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is AttentionShiftingTaskState.Running -> {
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
                    if (stimulusVisible && currentShape != null && currentColor != null) {
                        // Draw the shape based on stimulus properties
                        val shapeColor = when (currentColor) {
                            "blue" -> Color(0xFF2196F3)
                            "red" -> Color(0xFFF44336)
                            else -> Color.Gray
                        }

                        if (currentShape == "square") {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .background(shapeColor)
                            )
                        } else { // circle
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape)
                                    .background(shapeColor)
                            )
                        }
                    }
                }

                // Current rule instruction
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    val ruleText = when (currentRule) {
                        Rule.COLOR -> "Current Rule: Tap when you see BLUE shapes"
                        Rule.SHAPE -> "Current Rule: Tap when you see SQUARE shapes"
                    }

                    InstructionBar(text = ruleText)
                }
            }
        }

        is AttentionShiftingTaskState.Completed -> {
            // Navigate to results
            LaunchedEffect(state) {
                delay(100) // Small delay to ensure ViewModel state is stable
                navController.navigate("results/AttentionShifting") {
                    // Clear back stack so user can't go back to the task
                    popUpTo("home")
                }
            }

            // Loading indicator while navigating
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}