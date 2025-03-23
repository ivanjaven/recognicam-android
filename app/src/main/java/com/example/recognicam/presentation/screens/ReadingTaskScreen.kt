package com.example.recognicam.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.viewmodel.ReadingTaskResultUI
import com.example.recognicam.presentation.viewmodel.ReadingTaskState
import com.example.recognicam.presentation.viewmodel.ReadingTaskViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingTaskScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: ReadingTaskViewModel = viewModel(
        factory = ReadingTaskViewModel.Factory(context)
    )

    // Check if camera permission is already granted
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    val uiState by viewModel.uiState.collectAsState()
    val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsState()

    // Create and remember the camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Clean up the executor when the composable leaves composition
    DisposableEffect(key1 = Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    when (val state = uiState) {
        is ReadingTaskState.PreInstructions -> {
            QuickAssessmentInstructions(
                taskName = "Reading Assessment",
                onBeginAssessment = { viewModel.proceedToTaskInstructions() }
            )
        }
        is ReadingTaskState.Instructions -> {
            // Existing Instructions code stays as is
            TaskInstructions(
                title = "Reading Assessment",
                instructions = listOf(
                    "You will be presented with a story to read at your own pace.",
                    "After reading, you'll answer questions about the story.",
                    "Take your time to understand what you read, but read at a natural pace.",
                    "Try to stay focused on the story."
                ),
                buttonText = if (!cameraPermissionGranted) "Request Camera Permission" else "Start Task",
                onButtonPress = {
                    if (!cameraPermissionGranted) {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        viewModel.startCountdown()
                    }
                }
            )
        }

        is ReadingTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is ReadingTaskState.Reading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // Invisible camera view to process facial data if permission granted
                if (cameraPermissionGranted) {
                    AndroidView(
                        modifier = Modifier.size(1.dp), // Effectively invisible
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder()
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                                            viewModel.processFaceImage(imageProxy)
                                        }
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        CameraActiveIndicator(
                            modifier = Modifier.padding(top = 8.dp, end = 16.dp)
                        )
                    }
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
                                        style = MaterialTheme.typography.headlineMedium,
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
        }

        is ReadingTaskState.Questions -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // Keep the invisible camera view active during questions too
                if (cameraPermissionGranted) {
                    AndroidView(
                        modifier = Modifier.size(1.dp),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder()
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                                            viewModel.processFaceImage(imageProxy)
                                        }
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        CameraActiveIndicator(
                            modifier = Modifier.padding(top = 8.dp, end = 16.dp)
                        )
                    }
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
                                            viewModel.selectAnswer(index)
                                        }
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

                    // Navigation buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous button (only visible after first question)
                        if (state.currentIndex > 0) {
                            OutlinedButton(
                                onClick = { viewModel.previousQuestion() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = "Previous",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Next/Finish button
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            enabled = selectedOptionIndex != null
                        ) {
                            Text(
                                text = if (state.currentIndex < state.totalQuestions - 1) "Next" else "Finish",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        is ReadingTaskState.Completed -> {
            ReadingResultsScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@Composable
fun ReadingResultsScreen(
    result: ReadingTaskResultUI,
    onBackToHome: () -> Unit
) {
    TaskResultsView(
        title = "Reading Assessment",
        adhdScore = result.adhdAssessment.adhdProbabilityScore,
        confidenceLevel = result.adhdAssessment.confidenceLevel,
        attentionScore = result.adhdAssessment.attentionScore,
        hyperactivityScore = result.adhdAssessment.hyperactivityScore,
        impulsivityScore = result.adhdAssessment.impulsivityScore,
        faceMetrics = result.faceMetrics,
        motionMetrics = result.motionMetrics,
        behavioralMarkers = result.adhdAssessment.behavioralMarkers,
        onBackToHome = onBackToHome
    ) {
        // Reading-specific performance metrics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Reading Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "How you performed on the reading comprehension task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Reading metrics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.readingSpeed} wpm",
                        label = "Reading Speed",
                        description = "Words per minute",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.comprehensionScore}%",
                        label = "Comprehension",
                        description = "Understanding",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.readingTime}s",
                        label = "Reading Time",
                        description = "Total duration",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.correctAnswers}",
                        label = "Correct",
                        description = "Questions",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.incorrectAnswers}",
                        label = "Incorrect",
                        description = "Questions",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.faceMetrics.lookAwayCount}",
                        label = "Look Aways",
                        description = "Distractions",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}