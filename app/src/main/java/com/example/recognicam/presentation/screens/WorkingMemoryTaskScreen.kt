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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.AnimatedStimulus
import com.example.recognicam.presentation.components.CameraActiveIndicator
import com.example.recognicam.presentation.components.CountdownTimer
import com.example.recognicam.presentation.components.InstructionBar
import com.example.recognicam.presentation.components.QuickAssessmentInstructions
import com.example.recognicam.presentation.components.ResultMetricItem
import com.example.recognicam.presentation.components.TaskInstructions
import com.example.recognicam.presentation.components.TaskResultsView
import com.example.recognicam.presentation.viewmodel.WorkingMemoryTaskResultUI
import com.example.recognicam.presentation.viewmodel.WorkingMemoryTaskState
import com.example.recognicam.presentation.viewmodel.WorkingMemoryTaskViewModel
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun WorkingMemoryTaskScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: WorkingMemoryTaskViewModel = viewModel(
        factory = WorkingMemoryTaskViewModel.Factory(context)
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

    when (val state = uiState) {
        is WorkingMemoryTaskState.PreInstructions -> {
            QuickAssessmentInstructions(
                taskName = "Working Memory Task",
                onBeginAssessment = { viewModel.proceedToTaskInstructions() }
            )
        }
        is WorkingMemoryTaskState.Instructions -> {
            // Existing Instructions code stays as is
            TaskInstructions(
                title = "Working Memory Task",
                instructions = listOf(
                    "You will see a sequence of shapes one at a time.",
                    "TAP when the current shape matches the one you saw immediately before it.",
                    "DO NOT TAP if the shape is different from the previous one.",
                    "This task measures your working memory ability."
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


        is WorkingMemoryTaskState.Countdown -> {
            CountdownTimer(
                initialValue = state.count,
                onComplete = { /* ViewModel handles this */ }
            )
        }

        is WorkingMemoryTaskState.Running -> {
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
                    AnimatedStimulus(
                        visible = stimulusVisible,
                        content = {
                            Text(
                                text = stimulus ?: "",
                                fontSize = 120.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
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
            WorkingMemoryResultsScreen(
                result = state.result,
                onBackToHome = { navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }}
            )
        }
    }
}

@Composable
fun WorkingMemoryResultsScreen(
    result: WorkingMemoryTaskResultUI,
    onBackToHome: () -> Unit
) {
    TaskResultsView(
        title = "Working Memory Task",
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
        // Working Memory specific performance metrics
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
                    text = "How you performed on the working memory task.",
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
                        value = "${result.correctResponses}",
                        label = "Correct",
                        description = "Matches detected",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.incorrectResponses}",
                        label = "Incorrect",
                        description = "False positives",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.missedResponses}",
                        label = "Missed",
                        description = "Missed matches",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ResultMetricItem(
                        value = "${result.memorySpan}-back",
                        label = "Memory Level",
                        description = "Task difficulty",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.faceMetrics.lookAwayCount}",
                        label = "Look Aways",
                        description = "Distractions",
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