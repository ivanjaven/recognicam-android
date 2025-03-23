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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.viewmodel.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun AttentionShiftingTaskScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: AttentionShiftingTaskViewModel = viewModel(
        factory = AttentionShiftingTaskViewModel.Factory(context)
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

    // Configure task for regular testing
    LaunchedEffect(Unit) {
        viewModel.configureDuration(120) // 60 seconds for regular testing
    }

    val uiState by viewModel.uiState.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val currentShape by viewModel.currentShape.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()
    val currentRule by viewModel.currentRule.collectAsState()

    // Create and remember the camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Clean up the executor when the composable leaves composition
    DisposableEffect(key1 = Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    when (val state = uiState) {
        is AttentionShiftingTaskState.PreInstructions -> {
            QuickAssessmentInstructions(
                taskName = "Attention Shifting Task",
                onBeginAssessment = { viewModel.proceedToTaskInstructions() }
            )
        }
        is AttentionShiftingTaskState.Instructions -> {
            // Existing Instructions code stays as is
            TaskInstructions(
                title = "Attention Shifting Task",
                instructions = listOf(
                    "In this task, you'll need to tap the screen based on changing rules.",
                    "First rule: Tap when you see BLUE shapes.",
                    "Second rule: Tap when you see SQUARE shapes.",
                    "The rule will change periodically. Pay attention to the instruction at the bottom of the screen."
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

        is AttentionShiftingTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is AttentionShiftingTaskState.Running -> {
            val ruleJustChanged by viewModel.ruleJustChanged.collectAsState()

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

                // Timer display
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CameraActiveIndicator(
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    Text(
                        text = "${timeRemaining}s",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

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

                // Rule change overlay
                RuleChangeOverlay(
                    isVisible = ruleJustChanged,
                    newRule = currentRule,
                    onAnimationComplete = { viewModel.acknowledgeRuleChange() }
                )

                // Current rule instruction - Enhanced for better visibility
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    val ruleText = when (currentRule) {
                        Rule.COLOR -> "Current Rule: Tap when you see BLUE shapes"
                        Rule.SHAPE -> "Current Rule: Tap when you see SQUARE shapes"
                    }

                    // Make the instruction bar more prominent based on current rule
                    val backgroundColor = when (currentRule) {
                        Rule.COLOR -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        Rule.SHAPE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ruleText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is AttentionShiftingTaskState.Completed -> {
            AttentionShiftingResultsScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@Composable
fun AttentionShiftingResultsScreen(
    result: AttentionShiftingTaskResultUI,
    onBackToHome: () -> Unit
) {
    TaskResultsView(
        title = "Attention Shifting Task",
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
        // Attention Shifting specific performance metrics
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
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "How you performed on the cognitive flexibility task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Performance metrics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.accuracy}%",
                        label = "Accuracy",
                        description = "Overall",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.averageResponseTime}ms",
                        label = "Response Time",
                        description = "Average",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.responseTimeVariability.roundToInt()}",
                        label = "Variability",
                        description = "Consistency",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.shiftingCost}ms",
                        label = "Shifting Cost",
                        description = "Rule change delay",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.ruleShifts}",
                        label = "Rule Changes",
                        description = "Total shifts",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.correctResponses}",
                        label = "Correct",
                        description = "Responses",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.incorrectResponses}",
                        label = "Incorrect",
                        description = "Responses",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.missedResponses}",
                        label = "Missed",
                        description = "Responses",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.faceMetrics.sustainedAttentionScore}%",
                        label = "Attention",
                        description = "Focus level",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}