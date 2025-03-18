package com.example.recognicam.presentation.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.core.utils.getInterpretationText
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.viewmodel.CPTTaskState
import com.example.recognicam.presentation.viewmodel.CPTTaskViewModel
import com.example.recognicam.presentation.viewmodel.EnhancedCPTTaskResult
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun CPTTaskScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: CPTTaskViewModel = viewModel(
        factory = CPTTaskViewModel.Factory(context)
    )

    // Use a simple boolean state for permission instead of accompanist
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    // Request permission when the screen is first shown
    LaunchedEffect(Unit) {
        // In a real app, you would check permission here using ActivityResultLauncher
        // For demo purposes, let's assume permission is granted
        cameraPermissionGranted = true

        viewModel.setTaskDuration(60) // 60 seconds for regular testing
    }

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is CPTTaskState.Instructions -> {
            TaskInstructionsScreen(
                cameraPermissionGranted = cameraPermissionGranted,
                onRequestPermission = {
                    // In a real app, you would launch permission request here
                    cameraPermissionGranted = true
                },
                onStartTask = { viewModel.startCountdown() }
            )
        }

        is CPTTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is CPTTaskState.Running -> {
            CPTTaskRunningScreen(
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner,
                cameraPermissionGranted = cameraPermissionGranted
            )
        }

        is CPTTaskState.Completed -> {
            EnhancedResultsScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@Composable
fun TaskInstructionsScreen(
    cameraPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onStartTask: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!cameraPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera permission is required for face analysis",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRequestPermission
                ) {
                    Text("Request Permission")
                }
            }
        } else {
            TaskInstructions(
                title = "Continuous Performance Test",
                instructions = listOf(
                    "In this task, you will see letters appear on the screen one at a time.",
                    "Tap the screen whenever you see the letter X.",
                    "Try to respond as quickly and accurately as possible.",
                    "Your face and movements will be analyzed to detect ADHD patterns."
                ),
                onButtonPress = onStartTask
            )
        }
    }
}
@Composable
fun CPTTaskRunningScreen(
    viewModel: CPTTaskViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraPermissionGranted: Boolean
) {
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val stimulus by viewModel.stimulus.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()
    val faceMetrics by viewModel.faceMetrics.collectAsState()
    val motionMetrics by viewModel.motionMetrics.collectAsState()

    // Create and remember the camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Clean up the executor when the composable leaves composition
    DisposableEffect(key1 = Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background camera view for face analysis
        if (cameraPermissionGranted) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
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
                                it.setAnalyzer(cameraExecutor, { imageProxy ->
                                    viewModel.processFaceImage(imageProxy)
                                })
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

        // Semi-transparent overlay for the task
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { viewModel.handleResponse() }
        ) {
            // Timer display
            Text(
                text = "${timeRemaining}s",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
            )

            // Live metrics (small display in corner)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Look Aways: ${faceMetrics.lookAwayCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Blinks: ${faceMetrics.blinkCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Fidget: ${motionMetrics.fidgetingScore}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Main stimulus in center
            AnimatedStimulus(
                visible = stimulusVisible,
                content = {
                    Text(
                        text = stimulus?.toString() ?: "",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                modifier = Modifier.align(Alignment.Center)
            )

            // Bottom instruction bar
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                InstructionBar(text = "Tap when you see the letter X")
            }
        }
    }
}

@Composable
fun EnhancedResultsScreen(
    result: EnhancedCPTTaskResult,
    onBackToHome: () -> Unit
) {
    val assessment = result.adhdAssessment
    val scrollState = rememberScrollState()

    // Animation for score display
    var showScores by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        showScores = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Assessment Results",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Continuous Performance Test",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Primary scores
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1000))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ADHD Probability Score
                    ScoreCircle(
                        score = assessment.adhdProbabilityScore,
                        label = "ADHD Probability",
                        color = getScoreColor(assessment.adhdProbabilityScore),
                        size = 120.dp
                    )

                    // Confidence Level
                    ScoreCircle(
                        score = assessment.confidenceLevel,
                        label = "Confidence",
                        color = when {
                            assessment.confidenceLevel >= 80 -> Color(0xFF43A047)
                            assessment.confidenceLevel >= 60 -> Color(0xFF7CB342)
                            else -> Color(0xFFFFA726)
                        },
                        size = 120.dp
                    )
                }
            }

            // Domain scores
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1200, delayMillis = 300))
            ) {
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
                            text = "ADHD Domain Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        DomainScoreBar(
                            score = assessment.attentionScore,
                            label = "Inattention",
                            color = Color(0xFFE57373)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DomainScoreBar(
                            score = assessment.hyperactivityScore,
                            label = "Hyperactivity",
                            color = Color(0xFF64B5F6)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DomainScoreBar(
                            score = assessment.impulsivityScore,
                            label = "Impulsivity",
                            color = Color(0xFFFFB74D)
                        )
                    }
                }
            }

            // Performance metrics
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1200, delayMillis = 600))
            ) {
                PerformanceMetricsCard(result)
            }

            // Behavioral markers
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1200, delayMillis = 900))
            ) {
                BehavioralMarkersCard(assessment)
            }

            // Sensor metrics
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1200, delayMillis = 1200))
            ) {
                SensorMetricsCard(result)
            }

            // Interpretation
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1200, delayMillis = 1500))
            ) {
                InterpretationCard(assessment)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back button
            Button(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

@Composable
fun PerformanceMetricsCard(result: EnhancedCPTTaskResult) {
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
                text = "Task Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Performance metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultMetricItem(
                    value = "${result.accuracy}%",
                    label = "Accuracy",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.averageResponseTime}ms",
                    label = "Response Time",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.responseTimeVariability.roundToInt()}",
                    label = "Variability",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultMetricItem(
                    value = "${result.correctResponses}",
                    label = "Correct",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.incorrectResponses}",
                    label = "Incorrect",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.missedResponses}",
                    label = "Missed",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BehavioralMarkersCard(assessment: com.example.recognicam.data.analysis.ADHDAssessmentResult) {
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
                text = "Key Behavioral Markers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show top 5 significant markers
            val topMarkers = assessment.behavioralMarkers
                .sortedByDescending { it.significance }
                .take(5)

            topMarkers.forEach { marker ->
                BehavioralMarkerItem(marker = marker)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SensorMetricsCard(result: EnhancedCPTTaskResult) {
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
                text = "Sensor Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Face metrics
            Text(
                text = "Face Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultMetricItem(
                    value = "${result.faceMetrics.lookAwayCount}",
                    label = "Look Aways",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.faceMetrics.blinkRate.roundToInt()}/min",
                    label = "Blink Rate",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.faceMetrics.faceVisiblePercentage}%",
                    label = "Face Detected",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motion metrics
            Text(
                text = "Motion Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultMetricItem(
                    value = "${result.motionMetrics.fidgetingScore}%",
                    label = "Fidgeting",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.motionMetrics.restlessness}%",
                    label = "Restlessness",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.motionMetrics.suddenMovements}",
                    label = "Sudden Moves",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InterpretationCard(assessment: com.example.recognicam.data.analysis.ADHDAssessmentResult) {
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
                text = "What This Means",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = getInterpretationText(assessment.adhdProbabilityScore),
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
}