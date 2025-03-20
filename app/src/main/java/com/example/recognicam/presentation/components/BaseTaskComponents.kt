package com.example.recognicam.presentation.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.recognicam.data.analysis.BehavioralMarker
import com.example.recognicam.data.sensor.FaceMetrics
import com.example.recognicam.data.sensor.MotionMetrics
import com.example.recognicam.presentation.theme.Error
import com.example.recognicam.presentation.theme.Info
import com.example.recognicam.presentation.theme.Warning
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Shared components for assessment task screens
 */

// Base results view for any assessment task
@Composable
fun TaskResultsView(
    title: String,
    adhdScore: Int,
    confidenceLevel: Int,
    attentionScore: Int,
    hyperactivityScore: Int,
    impulsivityScore: Int,
    faceMetrics: FaceMetrics,
    motionMetrics: MotionMetrics,
    behavioralMarkers: List<BehavioralMarker>,
    onBackToHome: () -> Unit,
    performanceContent: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    // Animation for score display
    var showScores by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        showScores = true
    }

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
            text = title,
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
                    score = adhdScore,
                    label = "ADHD Probability",
                    color = assessmentScoreColor(adhdScore),
                    size = 120.dp
                )

                // Confidence Level
                ScoreCircle(
                    score = confidenceLevel,
                    label = "Confidence",
                    color = assessmentScoreColor(100 - confidenceLevel), // Invert scale for confidence
                    size = 120.dp
                )
            }
        }

        // Explanation Card
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 200))
        ) {
            ResultsExplanationCard()
        }

        // Domain scores
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 300))
        ) {
            ResultsDomainCard(
                attentionScore = attentionScore,
                hyperactivityScore = hyperactivityScore,
                impulsivityScore = impulsivityScore
            )
        }

        // Task-specific performance metrics
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 600))
        ) {
            performanceContent()
        }

        // Behavioral markers
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 900))
        ) {
            BehavioralMarkersCard(behavioralMarkers)
        }

        // Sensor metrics
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 1200))
        ) {
            SensorMetricsCard(faceMetrics, motionMetrics)
        }

        // Interpretation
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 1500))
        ) {
            InterpretationCard(adhdScore)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Back button
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Back to Home",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsExplanationCard() {
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

            // More concise domain explanations
            Text(
                text = "• Inattention: Difficulty maintaining focus on tasks",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "• Hyperactivity: Excessive physical movement and restlessness",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "• Impulsivity: Acting without thinking of consequences",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "• Confidence: Reliability of the assessment data",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Note: Some movement and distraction is completely normal.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsDomainCard(
    attentionScore: Int,
    hyperactivityScore: Int,
    impulsivityScore: Int
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

            // Improved domain score bars with unfilled style
            DomainScoreBar(
                score = attentionScore,
                label = "Inattention",
                color = Error
            )

            Spacer(modifier = Modifier.height(12.dp))

            DomainScoreBar(
                score = hyperactivityScore,
                label = "Hyperactivity",
                color = Info
            )

            Spacer(modifier = Modifier.height(12.dp))

            DomainScoreBar(
                score = impulsivityScore,
                label = "Impulsivity",
                color = Warning
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehavioralMarkersCard(markers: List<BehavioralMarker>) {
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
            Text(
                text = "Key Behavioral Markers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Exclude metrics already shown in other sections
            val excludedMetrics = setOf(
                "Response Time", "Response Variability", "Task Accuracy",
                "Missed Responses", "Emotion Changes", "Face Visibility",
                "Sustained Attention", "Fidgeting Score", "Sudden Movements"
            )

            // Show only unique behavioral markers not shown elsewhere
            val uniqueMarkers = markers
                .filter { it.name !in excludedMetrics }
                .sortedByDescending {
                    // Sort by significance * ratio to threshold
                    it.significance * (it.value / it.threshold)
                }
                .take(6)

            if (uniqueMarkers.isEmpty()) {
                Text(
                    text = "See Task Performance and Sensor Analysis for additional markers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                uniqueMarkers.forEach { marker ->
                    BehavioralMarkerItem(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorMetricsCard(faceMetrics: FaceMetrics, motionMetrics: MotionMetrics) {
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
                    value = "${faceMetrics.lookAwayCount}",
                    label = "Look Aways",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${faceMetrics.blinkRate.roundToInt()}/min",
                    label = "Blink Rate",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${faceMetrics.faceVisiblePercentage}%",
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
                    value = "${faceMetrics.distractibilityIndex}%",
                    label = "Distractibility",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${faceMetrics.sustainedAttentionScore}%",
                    label = "Attention",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${faceMetrics.emotionVariabilityScore}%",
                    label = "Emotion Changes",
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
                    value = "${motionMetrics.fidgetingScore}%",
                    label = "Fidgeting",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${motionMetrics.restlessness}%",
                    label = "Restlessness",
                    modifier = Modifier.weight(1f)
                )

                ResultMetricItem(
                    value = "${motionMetrics.suddenMovements}",
                    label = "Sudden Moves",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpretationCard(adhdScore: Int) {
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
                text = "This screening measures behavioral patterns associated with ADHD. " +
                        "It is not a diagnosis, but may identify behaviors worth discussing with a professional.",
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
                text = com.example.recognicam.core.utils.getInterpretationText(adhdScore),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Clinical note - more concise
            Text(
                text = "Note: This is not a clinical diagnosis. Consult a healthcare professional for concerns about ADHD.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}