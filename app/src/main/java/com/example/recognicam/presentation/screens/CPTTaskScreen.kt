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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.recognicam.core.utils.getMetricDescription
import com.example.recognicam.data.analysis.BehavioralMarker
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.viewmodel.CPTTaskState
import com.example.recognicam.presentation.viewmodel.CPTTaskViewModel
import com.example.recognicam.presentation.viewmodel.CPTTaskResult
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
        viewModel.setTaskDuration(30) // 30 seconds for faster testing
    }

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is CPTTaskState.Instructions -> {
            TaskInstructionsScreen(
                cameraPermissionGranted = cameraPermissionGranted,
                onRequestPermission = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) },
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
            ResultsScreen(
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
                    "Your face and movements will be analyzed to detect ADHD patterns.",
                    "This test will take 30 seconds to complete."
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

    // Create and remember the camera executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Clean up the executor when the composable leaves composition
    DisposableEffect(key1 = Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Style similar to GoNoGo and WorkingMemory tasks
    Box(modifier = Modifier.fillMaxSize()) {
        // Solid color background similar to other tasks
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

        // Task UI - similar to other tasks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { viewModel.handleResponse() }
        ) {
            // Timer display (similar style to other tasks)
            Text(
                text = "${timeRemaining}s",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
            )

            // Main stimulus in center - make sure it's highly visible
            if (stimulusVisible) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
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

            // Bottom instruction bar (similar to other tasks)
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                InstructionBar(text = "Tap when you see the letter X")
            }
        }
    }
}

@Composable
fun ResultsScreen(
    result: CPTTaskResult,
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

            // Explanation Card (NEW)
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 200))
            ) {
                MetricsExplanationCard()
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
                            color = Color(0xFFE57373),
                            description = getMetricDescription("Inattention")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DomainScoreBar(
                            score = assessment.hyperactivityScore,
                            label = "Hyperactivity",
                            color = Color(0xFF64B5F6),
                            description = getMetricDescription("Hyperactivity")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DomainScoreBar(
                            score = assessment.impulsivityScore,
                            label = "Impulsivity",
                            color = Color(0xFFFFB74D),
                            description = getMetricDescription("Impulsivity")
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
fun PerformanceMetricsCard(result: CPTTaskResult) {
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

@Composable
fun BehavioralMarkersCard(assessment: com.example.recognicam.data.analysis.ADHDAssessmentResult) {
    var expandedMarker by remember { mutableStateOf<String?>(null) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Key Behavioral Markers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Info icon with tooltip
                IconButton(
                    onClick = { /* Show explanation dialog */ },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Information about behavioral markers",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "These specialized markers identify ADHD-associated behavioral patterns. Tap any marker for more information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Exclude metrics already shown in other sections
            val excludedMetrics = setOf(
                "Response Time",
                "Response Variability",
                "Task Accuracy",
                "Missed Responses",
                "Face Visibility",
                "Sustained Attention",
                "Fidgeting Score",
                "Sudden Movements"
            )

            // Show only unique behavioral markers not shown elsewhere
            val uniqueMarkers = assessment.behavioralMarkers
                .filter { it.name !in excludedMetrics }
                .sortedByDescending {
                    // Sort by significance * ratio to threshold
                    it.significance * (it.value / it.threshold)
                }
                .take(6)

            if (uniqueMarkers.isEmpty()) {
                Text(
                    text = "Additional behavioral markers are shown in the Task Performance and Sensor Analysis sections.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                uniqueMarkers.forEach { marker ->
                    EnhancedBehavioralMarkerItem(
                        marker = marker,
                        isExpanded = expandedMarker == marker.name,
                        onExpandToggle = {
                            expandedMarker = if (expandedMarker == marker.name) null else marker.name
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EnhancedBehavioralMarkerItem(
    marker: BehavioralMarker,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    // Determine if high values are good or bad for this metric
    val isHighValueGood = when (marker.name) {
        "Sustained Attention", "Task Accuracy", "Face Visibility" -> true
        else -> false
    }

    // Calculate normalized percentage for coloring
    val percentValue = (marker.value / marker.threshold).coerceIn(0f, 2f) * 50f

    // Determine color based on whether high values are good or bad
    val markerColor = if (isHighValueGood) {
        // For metrics where high values are GOOD (like sustained attention)
        when {
            percentValue > 75 -> Color(0xFF43A047)  // Green (good)
            percentValue > 50 -> Color(0xFFFFA726)  // Orange (moderate)
            else -> Color(0xFFE53935)               // Red (concerning)
        }
    } else {
        // For metrics where high values are BAD (like distractibility)
        when {
            percentValue > 75 -> Color(0xFFE53935)  // Red (concerning)
            percentValue > 50 -> Color(0xFFFFA726)  // Orange (moderate)
            else -> Color(0xFF43A047)               // Green (good)
        }
    }

    // Determine significance (dots) based on value compared to threshold
    val significance = if (isHighValueGood) {
        // For metrics where high values are GOOD
        when {
            percentValue > 75 -> 1  // Low significance (good performance)
            percentValue > 50 -> 2  // Medium significance
            else -> 3               // High significance (poor performance)
        }
    } else {
        // For metrics where high values are BAD (keep existing logic)
        when {
            percentValue > 75 -> 3  // High significance (poor performance)
            percentValue > 50 -> 2  // Medium significance
            else -> 1               // Low significance (good performance)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator dots for significance with more informative color
            Row(
                modifier = Modifier.width(36.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < significance)
                                    markerColor
                                else
                                    Color.LightGray.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = marker.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress bar - color based on whether high values are good/bad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentValue / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(markerColor)
                    )
                }
            }

            // Value with interpretation text
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(70.dp)
            ) {
                Text(
                    text = if (marker.value >= 100) "${marker.value.toInt()}" else "${marker.value}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )

                // Determine rating text based on whether high values are good/bad
                val rating = if (isHighValueGood) {
                    when {
                        percentValue > 75 -> "Good"
                        percentValue > 50 -> "Moderate"
                        else -> "Low"
                    }
                } else {
                    when {
                        percentValue > 75 -> "High"
                        percentValue > 50 -> "Moderate"
                        else -> "Normal"
                    }
                }

                Text(
                    text = rating,
                    style = MaterialTheme.typography.bodySmall,
                    color = markerColor,
                    textAlign = TextAlign.End
                )
            }
        }

        // Expanded description
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = marker.description.ifEmpty { getMetricDescription(marker.name) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 36.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun SensorMetricsCard(result: CPTTaskResult) {
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
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Measurements from facial recognition and motion sensors.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
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

            Spacer(modifier = Modifier.height(8.dp))

            // Additional face metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultMetricItem(
                    value = "${result.faceMetrics.distractibilityIndex}%",
                    label = "Distractibility",
                    description = "Higher values indicate more frequent attention breaks",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.faceMetrics.sustainedAttentionScore}%",
                    label = "Sustained Attention",
                    description = "Higher values indicate better focus maintenance",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.faceMetrics.emotionVariabilityScore}%",
                    label = "Emotion Changes",
                    description = "Frequency of facial expression changes",
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
                    description = "Small, repeated movements during the task",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.motionMetrics.restlessness}%",
                    label = "Restlessness",
                    description = "Overall physical activity level",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${result.motionMetrics.suddenMovements}",
                    label = "Sudden Moves",
                    description = "Quick, unexpected movements",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricsExplanationCard() {
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
                text = "Understanding the Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Domain explanations
            Text(
                text = "• Inattention: Difficulty focusing, staying on task, and completing work without being distracted.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "• Hyperactivity: Excessive movement, fidgeting, and physical restlessness during the task.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "• Impulsivity: Acting without fully thinking through consequences or ability to wait appropriately.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "• Confidence: How reliable the assessment data is, based on task completion and sensor data quality.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Note: Some movement, looking away, or response variation is completely normal and not cause for concern.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
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
            // Understanding the results section
            Text(
                text = "What This Means",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "This screening measures behavioral patterns frequently associated with ADHD. " +
                        "It is not a diagnosis, but can help identify behaviors worth discussing with a professional.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // What the score means
            Text(
                text = "Your Results",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = getInterpretationText(assessment.adhdProbabilityScore),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // What the behavioral markers mean
            Text(
                text = "About the Behavioral Markers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
            )

            Text(
                text = "The colored bars show how your behavior compares to typical patterns. " +
                        "Green always indicates typical (non-ADHD) patterns. " +
                        "For metrics like attention and accuracy, high values are good. " +
                        "For metrics like response time and fidgeting, low values are good.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Note: This is not a clinical diagnosis. If you have concerns about ADHD, please consult a healthcare professional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
@Composable
fun DomainScoreBar(
    score: Int,
    label: String,
    color: Color,
    description: String = ""
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (description.isNotEmpty()) {
                    IconButton(
                        onClick = { /* Show tooltip */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Information about $label",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "$score%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Score bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
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
    }
}

@Composable
fun ResultMetricItem(
    value: String,
    label: String,
    description: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun getScoreColor(score: Int): Color {
    return when {
        score >= 70 -> Color(0xFFE53935) // High (red)
        score >= 40 -> Color(0xFFFFA726) // Medium (orange)
        else -> Color(0xFF43A047)        // Low (green)
    }
}