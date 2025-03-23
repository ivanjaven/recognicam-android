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
import com.example.recognicam.presentation.viewmodel.GoNoGoTaskResultUI
import com.example.recognicam.presentation.viewmodel.GoNoGoTaskState
import com.example.recognicam.presentation.viewmodel.GoNoGoTaskViewModel
import com.example.recognicam.presentation.viewmodel.StimulusType
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun GoNoGoTaskScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: GoNoGoTaskViewModel = viewModel(
        factory = GoNoGoTaskViewModel.Factory(context)
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
        viewModel.configureDuration(40) // 40 seconds for regular testing
    }

    val uiState by viewModel.uiState.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val stimulusType by viewModel.stimulusType.collectAsState()
    val stimulusVisible by viewModel.stimulusVisible.collectAsState()

    // Create and remember the camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Clean up the executor when the composable leaves composition
    DisposableEffect(key1 = Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    when (val state = uiState) {
        is GoNoGoTaskState.PreInstructions -> {
            QuickAssessmentInstructions(
                taskName = "Go/No-Go Task",
                onBeginAssessment = { viewModel.proceedToTaskInstructions() }
            )
        }
        is GoNoGoTaskState.Instructions -> {
            // Existing Instructions code stays as is
            TaskInstructions(
                title = "Go/No-Go Task",
                instructions = listOf(
                    "In this task, you will see green and red circles appear on the screen.",
                    "TAP when you see a GREEN circle (GO).",
                    "DO NOT TAP when you see a RED circle (NO-GO).",
                    "Try to respond as quickly and accurately as possible."
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

        is GoNoGoTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is GoNoGoTaskState.Running -> {
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
                    if (stimulusVisible && stimulusType != null) {
                        // Show colored circle based on stimulus type
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(
                                    when (stimulusType) {
                                        StimulusType.GO -> Color(0xFF4CAF50) // Green
                                        StimulusType.NO_GO -> Color(0xFFF44336) // Red
                                        null -> Color.LightGray
                                    }
                                )
                        )
                    }
                }

                // Bottom instruction bar
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    InstructionBar(text = "Tap for Green • Don't Tap for Red")
                }
            }
        }

        is GoNoGoTaskState.Completed -> {
            GoNoGoResultsScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@Composable
fun GoNoGoResultsScreen(
    result: GoNoGoTaskResultUI,
    onBackToHome: () -> Unit
) {
    TaskResultsView(
        title = "Go/No-Go Task",
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
        // Go/No-Go specific performance metrics
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
                    text = "How you performed on the inhibition control task.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Performance metrics grid - top row
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

                // Second row - correct responses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.correctGo}",
                        label = "Correct Go",
                        description = "Green taps",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.correctNoGo}",
                        label = "Correct No-Go",
                        description = "Red inhibits",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.incorrectNoGo}",
                        label = "Impulse Errors",
                        description = "Red taps",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}