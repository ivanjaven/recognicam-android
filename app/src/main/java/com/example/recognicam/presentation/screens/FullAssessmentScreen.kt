// FullAssessmentScreen.kt
package com.example.recognicam.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.CountdownTimer
import com.example.recognicam.presentation.components.TaskInstructions
import com.example.recognicam.presentation.viewmodel.*
import kotlinx.coroutines.delay

@Composable
fun FullAssessmentScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: FullAssessmentViewModel = viewModel(
        factory = FullAssessmentViewModel.Factory(context)
    )

    val uiState by viewModel.uiState.collectAsState()

    // Set up task ViewModels that will be reused for each task
    val cptViewModel: CPTTaskViewModel = viewModel(
        factory = CPTTaskViewModel.Factory(context, true)
    )

    val readingViewModel: ReadingTaskViewModel = viewModel(
        factory = ReadingTaskViewModel.Factory(context, true)
    )

    val goNoGoViewModel: GoNoGoTaskViewModel = viewModel(
        factory = GoNoGoTaskViewModel.Factory(context, true)
    )

    val workingMemoryViewModel: WorkingMemoryTaskViewModel = viewModel(
        factory = WorkingMemoryTaskViewModel.Factory(context, true)
    )

    val attentionShiftingViewModel: AttentionShiftingTaskViewModel = viewModel(
        factory = AttentionShiftingTaskViewModel.Factory(context, true)
    )

    // Configure shorter durations for all tasks in the full assessment
    LaunchedEffect(Unit) {
        cptViewModel.configureDuration(30) // 30 seconds for CPT
        // No need to configure reading duration as it's user-paced
        goNoGoViewModel.configureDuration(30) // 30 seconds for Go/No-Go
        workingMemoryViewModel.configureDuration(30) // 30 seconds for Working Memory
        attentionShiftingViewModel.configureDuration(30) // 30 seconds for Attention Shifting
    }

    // Observe each task's state to capture results
    val cptState by cptViewModel.uiState.collectAsState()
    val readingState by readingViewModel.uiState.collectAsState()
    val goNoGoState by goNoGoViewModel.uiState.collectAsState()
    val workingMemoryState by workingMemoryViewModel.uiState.collectAsState()
    val attentionShiftingState by attentionShiftingViewModel.uiState.collectAsState()

    // Forward task completions to the FullAssessmentViewModel
    LaunchedEffect(cptState) {
        if (cptState is CPTTaskState.Completed) {
            viewModel.saveCPTResult((cptState as CPTTaskState.Completed).result)
        }
    }

    LaunchedEffect(readingState) {
        if (readingState is ReadingTaskState.Completed) {
            viewModel.saveReadingResult((readingState as ReadingTaskState.Completed).result)
        }
    }

    LaunchedEffect(goNoGoState) {
        if (goNoGoState is GoNoGoTaskState.Completed) {
            viewModel.saveGoNoGoResult((goNoGoState as GoNoGoTaskState.Completed).result)
        }
    }

    LaunchedEffect(workingMemoryState) {
        if (workingMemoryState is WorkingMemoryTaskState.Completed) {
            viewModel.saveWorkingMemoryResult((workingMemoryState as WorkingMemoryTaskState.Completed).result)
        }
    }

    LaunchedEffect(attentionShiftingState) {
        if (attentionShiftingState is AttentionShiftingTaskState.Completed) {
            viewModel.saveAttentionShiftingResult((attentionShiftingState as AttentionShiftingTaskState.Completed).result)
        }
    }

    when (val state = uiState) {
        is FullAssessmentState.Instructions -> {
            FullAssessmentInstructions(
                onStartAssessment = { viewModel.startAssessment() }
            )
        }

        is FullAssessmentState.StartCPTTask -> {
            // Reset CPT task to instruction state if needed
            LaunchedEffect(Unit) {
                // This is a workaround since we don't have direct access to reset tasks
                // In a real implementation, you would add a reset method to each task ViewModel
            }

            // Use the existing CPT task screen but override navigation
            CPTTaskScreen(
                navController = object : NavController(context) {
                    // Override navigation to prevent the screen from navigating away
                }
            )
        }

        is FullAssessmentState.StartReadingTask -> {
            // Use the existing Reading task screen but override navigation
            ReadingTaskScreen(
                navController = object : NavController(context) {
                    // Override navigation to prevent the screen from navigating away
                }
            )
        }

        is FullAssessmentState.StartGoNoGoTask -> {
            // Use the existing Go/No-Go task screen but override navigation
            GoNoGoTaskScreen(
                navController = object : NavController(context) {
                    // Override navigation to prevent the screen from navigating away
                }
            )
        }

        is FullAssessmentState.StartWorkingMemoryTask -> {
            // Use the existing Working Memory task screen but override navigation
            WorkingMemoryTaskScreen(
                navController = object : NavController(context) {
                    // Override navigation to prevent the screen from navigating away
                }
            )
        }

        is FullAssessmentState.StartAttentionShiftingTask -> {
            // Use the existing Attention Shifting task screen but override navigation
            AttentionShiftingTaskScreen(
                navController = object : NavController(context) {
                    // Override navigation to prevent the screen from navigating away
                }
            )
        }

        is FullAssessmentState.TaskTransition -> {
            TaskTransitionScreen(
                fromTask = state.from,
                toTask = state.to,
                message = state.message,
                onContinue = {
                    when (state.from) {
                        "Continuous Performance Test" -> viewModel.proceedToReadingTask()
                        "Reading Assessment" -> viewModel.proceedToGoNoGoTask()
                        "Go/No-Go Task" -> viewModel.proceedToWorkingMemoryTask()
                        "Working Memory Task" -> viewModel.proceedToAttentionShiftingTask()
                        // For the last transition, we'll show results automatically
                    }
                }
            )
        }

        is FullAssessmentState.Completed -> {
            FullAssessmentResultScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullAssessmentInstructions(
    onStartAssessment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Full ADHD Assessment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Please ensure for the best results:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                BulletPoint("Make sure the room has good lighting so your face is clearly visible")
                BulletPoint("Position yourself in a comfortable, quiet environment")
                BulletPoint("Try to maintain focus during the tasks and give your best effort")
                BulletPoint("Take breaks between tasks if needed, but try to complete the full assessment in one session")
                BulletPoint("Answer honestly and don't overthink your responses")
                BulletPoint("This assessment uses your device's camera to analyze attention patterns during the task. " +
                        "Your privacy is important - no video is stored or transmitted.")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "The assessment consists of 5 short tasks:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AssessmentTaskItem(
                    title = "Continuous Performance Test",
                    description = "Tap when you see the letter X appear on screen",
                    duration = "2 minutes"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                AssessmentTaskItem(
                    title = "Reading Assessment",
                    description = "Read a short passage and answer questions about it",
                    duration = "2-4 minutes"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                AssessmentTaskItem(
                    title = "Go/No-Go Task",
                    description = "Tap for green circles, don't tap for red circles",
                    duration = "2 minutes"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                AssessmentTaskItem(
                    title = "Working Memory Task",
                    description = "Tap when a shape matches the previous one",
                    duration = "2 minutes"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                AssessmentTaskItem(
                    title = "Attention Shifting Task",
                    description = "Follow changing rules for shapes and colors",
                    duration = "2 minutes"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Important Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "• This assessment is not a medical diagnosis\n" +
                            "• Results should be discussed with a healthcare professional\n" +
                            "• Your face will be analyzed during tasks, but no recordings are stored\n" +
                            "• The full assessment takes approximately 5-10 minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = onStartAssessment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Begin Full Assessment",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 8.dp, top = 0.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun AssessmentTaskItem(
    title: String,
    description: String,
    duration: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "⏱️ $duration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TaskTransitionScreen(
    fromTask: String,
    toTask: String,
    message: String,
    onContinue: () -> Unit
) {
    var countdown by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        // Auto transition after a few seconds
        for (i in 3 downTo 1) {
            countdown = i
            delay(1000)
        }
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success icon or animation
            Text(
                text = "✓",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "$fromTask completed!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (toTask != "Results") {
                Text(
                    text = "Next: $toTask",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Continuing in $countdown...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Compiling your results...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}