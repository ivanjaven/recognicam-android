package com.example.recognicam.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.AnimatedStimulus
import com.example.recognicam.presentation.components.CountdownTimer
import com.example.recognicam.presentation.components.InstructionBar
import com.example.recognicam.presentation.components.TaskInstructions
import com.example.recognicam.presentation.viewmodel.WorkingMemoryTaskState
import com.example.recognicam.presentation.viewmodel.WorkingMemoryTaskViewModel
import kotlinx.coroutines.delay

@Composable
fun WorkingMemoryTaskScreen(
    navController: NavController,
    viewModel: WorkingMemoryTaskViewModel = viewModel()
) {
    // Configure task for regular testing
    LaunchedEffect(Unit) {
        viewModel.setTaskDuration(40) // 40 seconds for regular testing
    }

    val uiState by viewModel.uiState.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val stimulus by viewModel.stimulus.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()

    when (val state = uiState) {
        is WorkingMemoryTaskState.Instructions -> {
            TaskInstructions(
                title = "Working Memory Task",
                instructions = listOf(
                    "You will see a sequence of shapes one at a time.",
                    "TAP when the current shape matches the one you saw immediately before it.",
                    "DO NOT TAP if the shape is different from the previous one.",
                    "This task measures your working memory ability."
                ),
                onButtonPress = { viewModel.startCountdown() }
            )
        }

        is WorkingMemoryTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is WorkingMemoryTaskState.Running -> {
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
                    AnimatedStimulus(
                        visible = stimulusVisible,
                        content = {
                            Text(
                                text = stimulus ?: "",
                                fontSize = 120.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }

                // Bottom instruction bar
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    InstructionBar(text = "Tap when current shape matches previous shape")
                }
            }
        }

        is WorkingMemoryTaskState.Completed -> {
            // Navigate to results
            LaunchedEffect(state) {
                delay(100) // Small delay to ensure ViewModel state is stable
                navController.navigate("results/WorkingMemory") {
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