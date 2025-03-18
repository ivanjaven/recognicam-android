// ReadingTaskScreen.kt
package com.example.recognicam.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.domain.entity.ReadingTaskResult
import com.example.recognicam.presentation.components.CountdownTimer
import com.example.recognicam.presentation.components.TaskInstructions
import com.example.recognicam.presentation.viewmodel.ReadingTaskState
import com.example.recognicam.presentation.viewmodel.ReadingTaskViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTaskScreen(
    navController: NavController,
    viewModel: ReadingTaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsState()

    when (val state = uiState) {
        is ReadingTaskState.Instructions -> {
            TaskInstructions(
                title = "Reading Assessment",
                instructions = listOf(
                    "You will be presented with a passage to read at your own pace.",
                    "After reading, you'll answer questions about the content to assess your comprehension.",
                    "Take your time to understand the passage, but read at a natural pace."
                ),
                onButtonPress = { viewModel.startCountdown() }
            )
        }

        is ReadingTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { viewModel.onCountdownComplete() }
            )
        }

        is ReadingTaskState.Reading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Reading passage card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Animate the content appearing
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            isVisible = true
                        }

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(500))
                        ) {
                            Column {
                                Text(
                                    text = state.passage.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = state.passage.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                                )
                            }
                        }
                    }
                }

                // "Finished Reading" button
                Button(
                    onClick = { viewModel.finishReading() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "I've Finished Reading",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        is ReadingTaskState.Questions -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Question card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Question number
                        Text(
                            text = "Question ${state.currentIndex + 1} of ${state.totalQuestions}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Question text
                        Text(
                            text = state.question.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Answer options
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.question.options.forEachIndexed { index, option ->
                                val isSelected = selectedOptionIndex == index
                                val isDisabled = selectedOptionIndex != null

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 4.dp else 0.dp
                                    ),
                                    onClick = {
                                        if (!isDisabled) viewModel.selectAnswer(index)
                                    },
                                    enabled = !isDisabled
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Next button (only visible after selecting an option)
                AnimatedVisibility(visible = selectedOptionIndex != null) {
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (state.currentIndex < state.totalQuestions - 1) "Next Question" else "Finish",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        is ReadingTaskState.Completed -> {
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

private fun navigateToResults(navController: NavController, result: ReadingTaskResult) {
    navController.navigate("results/Reading") {
        // Clear back stack so user can't go back to the task
        popUpTo("home")
    }
}