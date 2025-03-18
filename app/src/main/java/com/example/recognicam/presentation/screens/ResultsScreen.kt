package com.example.recognicam.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recognicam.core.utils.getInterpretationText
import com.example.recognicam.core.utils.getScoreText
import com.example.recognicam.domain.entity.*
import com.example.recognicam.presentation.components.getScoreColor
import com.example.recognicam.presentation.viewmodel.ResultsUiState
import com.example.recognicam.presentation.viewmodel.ResultsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    taskType: String,
    viewModel: ResultsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is ResultsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ResultsUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error loading results",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }}
                ) {
                    Text(text = "Back to Home")
                }
            }
        }

        is ResultsUiState.Success -> {
            // Extract scores based on task type
            val adhdScore = when (val result = state.result) {
                is CPTTaskResult -> result.adhdProbabilityScore
                is ReadingTaskResult -> result.adhdProbabilityScore
                is GoNoGoTaskResult -> result.adhdProbabilityScore
                is WorkingMemoryTaskResult -> result.adhdProbabilityScore
                is AttentionShiftingTaskResult -> result.adhdProbabilityScore
                else -> 0
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "${state.taskType} Task Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Main content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ADHD Score card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ADHD Probability Score",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        getScoreColor(adhdScore),
                                        shape = RoundedCornerShape(50.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$adhdScore%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = getScoreText(adhdScore),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Performance metrics card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Performance Metrics",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Render different metrics based on task type
                            when (val result = state.result) {
                                is CPTTaskResult -> CPTResultMetrics(result)
                                is ReadingTaskResult -> ReadingResultMetrics(result)
                                is GoNoGoTaskResult -> GoNoGoResultMetrics(result)
                                is WorkingMemoryTaskResult -> WorkingMemoryResultMetrics(result)
                                is AttentionShiftingTaskResult -> AttentionShiftingResultMetrics(result)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Interpretation card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "What This Means",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = getInterpretationText(adhdScore),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Note: This is not a clinical diagnosis. If you have concerns about ADHD, please consult a healthcare professional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom button
                Button(
                    onClick = { navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Back to Home",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun CPTResultMetrics(result: CPTTaskResult) {
    MetricsGrid {
        SimpleMetricItem(label = "Accuracy", value = "${result.accuracy}%")
        SimpleMetricItem(label = "Avg. Response", value = "${result.averageResponseTime}ms")
        SimpleMetricItem(label = "Correct", value = "${result.correctResponses}")
        SimpleMetricItem(label = "Incorrect", value = "${result.incorrectResponses}")
        SimpleMetricItem(label = "Missed", value = "${result.missedResponses}")
        SimpleMetricItem(label = "Fidgeting", value = "${result.fidgetingScore}%")
    }
}

@Composable
fun ReadingResultMetrics(result: ReadingTaskResult) {
    MetricsGrid {
        SimpleMetricItem(label = "Reading Speed", value = "${result.readingSpeed} wpm")
        SimpleMetricItem(label = "Comprehension", value = "${result.comprehensionScore}%")
        SimpleMetricItem(label = "Reading Time", value = "${result.readingTime}s")
        SimpleMetricItem(label = "Correct Answers", value = "${result.correctAnswers}")
        SimpleMetricItem(label = "Incorrect", value = "${result.incorrectAnswers}")
    }
}

@Composable
fun GoNoGoResultMetrics(result: GoNoGoTaskResult) {
    MetricsGrid {
        SimpleMetricItem(label = "Accuracy", value = "${result.accuracy}%")
        SimpleMetricItem(label = "Avg. Response", value = "${result.averageResponseTime}ms")
        SimpleMetricItem(label = "Correct Go", value = "${result.correctGo}")
        SimpleMetricItem(label = "Correct No-Go", value = "${result.correctNoGo}")
        SimpleMetricItem(label = "Missed Go", value = "${result.missedGo}")
        SimpleMetricItem(label = "Incorrect No-Go", value = "${result.incorrectNoGo}")
    }
}

@Composable
fun WorkingMemoryResultMetrics(result: WorkingMemoryTaskResult) {
    MetricsGrid {
        SimpleMetricItem(label = "Accuracy", value = "${result.accuracy}%")
        SimpleMetricItem(label = "Avg. Response", value = "${result.averageResponseTime}ms")
        SimpleMetricItem(label = "Memory Span", value = "${result.memorySpan}-back")
        SimpleMetricItem(label = "Correct", value = "${result.correctResponses}")
        SimpleMetricItem(label = "Incorrect", value = "${result.incorrectResponses}")
        SimpleMetricItem(label = "Missed", value = "${result.missedResponses}")
    }
}

@Composable
fun AttentionShiftingResultMetrics(result: AttentionShiftingTaskResult) {
    MetricsGrid {
        SimpleMetricItem(label = "Accuracy", value = "${result.accuracy}%")
        SimpleMetricItem(label = "Avg. Response", value = "${result.averageResponseTime}ms")
        SimpleMetricItem(label = "Shifting Cost", value = "${result.shiftingCost}ms")
        SimpleMetricItem(label = "Correct", value = "${result.correctResponses}")
        SimpleMetricItem(label = "Incorrect", value = "${result.incorrectResponses}")
        SimpleMetricItem(label = "Missed", value = "${result.missedResponses}")
    }
}

@Composable
fun MetricsGrid(content: @Composable () -> Unit) {
    Column {
        content()
    }
}

@Composable
fun SimpleMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}