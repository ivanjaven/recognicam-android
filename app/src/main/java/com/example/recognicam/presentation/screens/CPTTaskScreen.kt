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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.viewmodel.CPTTaskState
import com.example.recognicam.presentation.viewmodel.CPTTaskViewModel
import com.example.recognicam.presentation.viewmodel.CPTTaskResult
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

    // Configure task for faster testing
    LaunchedEffect(Unit) {
        viewModel.configureDuration(30) // 30 seconds for faster testing
    }

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is CPTTaskState.PreInstructions -> {
            QuickAssessmentInstructions(
                taskName = "Continuous Performance Test",
                onBeginAssessment = { viewModel.proceedToTaskInstructions() }
            )
        }
        is CPTTaskState.Instructions -> {
            // Existing Instructions code stays as is
            TaskInstructions(
                title = "Continuous Performance Test",
                instructions = listOf(
                    "In this task, you will see letters appear on the screen one at a time.",
                    "Tap the screen whenever you see the letter X.",
                    "Try to respond as quickly and accurately as possible.",
                    "This test will take 30 seconds to complete."
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
        // Rest remains unchanged

        is CPTTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is CPTTaskState.Running -> {
            CPTTaskContent(
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner,
                cameraPermissionGranted = cameraPermissionGranted
            )
        }

        is CPTTaskState.Completed -> {
            CPTResultsScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@Composable
fun CPTTaskContent(
    viewModel: CPTTaskViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraPermissionGranted: Boolean
) {
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val stimulus by viewModel.stimulus.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()

    // Create and remember the camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Clean up the executor when the composable leaves composition
    DisposableEffect(key1 = Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )

        // Invisible camera view to process facial data
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

        // Task UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { viewModel.handleResponse() }
        ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Main stimulus in center
            if (stimulusVisible) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stimulus?.toString() ?: "",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Bottom instruction bar
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                InstructionBar(text = "Tap when you see the letter X")
            }
        }
    }
}

@Composable
fun CPTResultsScreen(
    result: CPTTaskResult,
    onBackToHome: () -> Unit
) {
    TaskResultsView(
        title = "Continuous Performance Test",
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
        // CPT-specific performance metrics
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
                    text = "How you performed on the attention task itself.",
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
}