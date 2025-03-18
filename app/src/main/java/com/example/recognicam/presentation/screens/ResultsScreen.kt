package com.example.recognicam.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recognicam.core.utils.getInterpretationText
import com.example.recognicam.core.utils.getMetricDescription
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

            val scrollState = rememberScrollState()

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
                        .verticalScroll(scrollState)
                ) {
// Enhanced ADHD Score card with explanation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ADHD Probability Score",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                // Info icon
                                IconButton(
                                    onClick = { /* Show info dialog */ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = "About this score",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

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
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = getScoreText(adhdScore),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Short explanation
                            Text(
                                text = "This score compares your behavioral patterns to those commonly associated with ADHD.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Task explanation card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "About This Assessment",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val taskDescription = when (taskType) {
                                "CPT" -> "The Continuous Performance Test (CPT) measures sustained attention and impulse control. It requires responding to specific targets while ignoring others."
                                "Reading" -> "The Reading Assessment evaluates attention and focus during reading comprehension, measuring both speed and understanding."
                                "GoNoGo" -> "The Go/No-Go Task tests impulse control and inhibition. It requires quick responses to 'go' signals while withholding responses to 'no-go' signals."
                                "WorkingMemory" -> "The Working Memory Task evaluates the ability to hold and manipulate information briefly in mind, a key executive function."
                                "AttentionShifting" -> "The Attention Shifting Task measures cognitive flexibility and the ability to adjust to changing rules, a common challenge in ADHD."
                                else -> "This task measures cognitive functions associated with attention and executive function."
                            }

                            Text(
                                text = taskDescription,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Performance metrics card with improved explanations
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
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "How you performed on this assessment task.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

                    // Interpretation card with improved explanations
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
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "Understanding These Results",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )

                            Text(
                                text = "• Everyone shows some ADHD-like behaviors occasionally.\n" +
                                        "• Results suggest patterns, not a diagnosis.\n" +
                                        "• Multiple assessments provide a more complete picture.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

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
        MetricItemWithDescription(
            label = "Accuracy",
            value = "${result.accuracy}%",
            description = "How many target letters you correctly identified."
        )
        MetricItemWithDescription(
            label = "Response Time",
            value = "${result.averageResponseTime}ms",
            description = "Average time to respond to targets. Slower times may suggest attention difficulties."
        )
        MetricItemWithDescription(
            label = "Correct",
            value = "${result.correctResponses}",
            description = "Times you correctly tapped when seeing the target letter."
        )
        MetricItemWithDescription(
            label = "Incorrect",
            value = "${result.incorrectResponses}",
            description = "Times you tapped when no target letter was shown."
        )
        MetricItemWithDescription(
            label = "Missed",
            value = "${result.missedResponses}",
            description = "Times you didn't tap when the target letter appeared."
        )
        MetricItemWithDescription(
            label = "Fidgeting",
            value = "${result.fidgetingScore}%",
            description = "Detected small, repetitive movements during the task."
        )
    }
}

@Composable
fun ReadingResultMetrics(result: ReadingTaskResult) {
    MetricsGrid {
        MetricItemWithDescription(
            label = "Reading Speed",
            value = "${result.readingSpeed} wpm",
            description = "Words per minute reading rate."
        )
        MetricItemWithDescription(
            label = "Comprehension",
            value = "${result.comprehensionScore}%",
            description = "How well you understood what you read."
        )
        MetricItemWithDescription(
            label = "Reading Time",
            value = "${result.readingTime}s",
            description = "Total time spent reading the passage."
        )
        MetricItemWithDescription(
            label = "Correct Answers",
            value = "${result.correctAnswers}",
            description = "Questions you answered correctly."
        )
        MetricItemWithDescription(
            label = "Incorrect",
            value = "${result.incorrectAnswers}",
            description = "Questions you answered incorrectly."
        )
    }
}

@Composable
fun GoNoGoResultMetrics(result: GoNoGoTaskResult) {
    MetricsGrid {
        MetricItemWithDescription(
            label = "Accuracy",
            value = "${result.accuracy}%",
            description = "Overall percentage of correct responses."
        )
        MetricItemWithDescription(
            label = "Response Time",
            value = "${result.averageResponseTime}ms",
            description = "Average time to respond to Go signals."
        )
        MetricItemWithDescription(
            label = "Correct Go",
            value = "${result.correctGo}",
            description = "Times you correctly tapped on green (Go)."
        )
        MetricItemWithDescription(
            label = "Correct No-Go",
            value = "${result.correctNoGo}",
            description = "Times you correctly didn't tap on red (No-Go)."
        )
        MetricItemWithDescription(
            label = "Missed Go",
            value = "${result.missedGo}",
            description = "Times you failed to tap on green (Go)."
        )
        MetricItemWithDescription(
            label = "Incorrect No-Go",
            value = "${result.incorrectNoGo}",
            description = "Times you tapped when shown red (No-Go). Higher values can indicate impulsivity."
        )
    }
}

@Composable
fun WorkingMemoryResultMetrics(result: WorkingMemoryTaskResult) {
    MetricsGrid {
        MetricItemWithDescription(
            label = "Accuracy",
            value = "${result.accuracy}%",
            description = "Overall percentage of correct responses."
        )
        MetricItemWithDescription(
            label = "Response Time",
            value = "${result.averageResponseTime}ms",
            description = "Average time to identify matching shapes."
        )
        MetricItemWithDescription(
            label = "Memory Span",
            value = "${result.memorySpan}-back",
            description = "How many items back you were asked to remember."
        )
        MetricItemWithDescription(
            label = "Correct",
            value = "${result.correctResponses}",
            description = "Times you correctly identified matching shapes."
        )
        MetricItemWithDescription(
            label = "Incorrect",
            value = "${result.incorrectResponses}",
            description = "Times you tapped when shapes didn't match."
        )
        MetricItemWithDescription(
            label = "Missed",
            value = "${result.missedResponses}",
            description = "Times you failed to tap when shapes matched."
        )
    }
}

@Composable
fun AttentionShiftingResultMetrics(result: AttentionShiftingTaskResult) {
    MetricsGrid {
        MetricItemWithDescription(
            label = "Accuracy",
            value = "${result.accuracy}%",
            description = "Overall percentage of correct responses."
        )
        MetricItemWithDescription(
            label = "Response Time",
            value = "${result.averageResponseTime}ms",
            description = "Average time to respond to targets."
        )
        MetricItemWithDescription(
            label = "Shifting Cost",
            value = "${result.shiftingCost}ms",
            description = "Extra time needed after rules changed. Higher values can indicate difficulty with cognitive flexibility."
        )
        MetricItemWithDescription(
            label = "Correct",
            value = "${result.correctResponses}",
            description = "Times you correctly followed the current rule."
        )
        MetricItemWithDescription(
            label = "Incorrect",
            value = "${result.incorrectResponses}",
            description = "Times you responded inappropriately to the current rule."
        )
        MetricItemWithDescription(
            label = "Missed",
            value = "${result.missedResponses}",
            description = "Times you failed to respond when you should have."
        )
    }
}

@Composable
fun MetricsGrid(content: @Composable () -> Unit) {
    Column {
        content()
    }
}

@Composable
fun MetricItemWithDescription(
    label: String,
    value: String,
    description: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
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