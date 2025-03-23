package com.example.recognicam.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recognicam.core.utils.getInterpretationText
import com.example.recognicam.core.utils.getScoreText
import com.example.recognicam.data.analysis.BehavioralMarker
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.theme.Error
import com.example.recognicam.presentation.theme.Info
import com.example.recognicam.presentation.theme.Success
import com.example.recognicam.presentation.theme.Warning
import com.example.recognicam.presentation.viewmodel.AttentionShiftingTaskResultUI
import com.example.recognicam.presentation.viewmodel.CPTTaskResult
import com.example.recognicam.presentation.viewmodel.FullAssessmentResult
import com.example.recognicam.presentation.viewmodel.GoNoGoTaskResultUI
import com.example.recognicam.presentation.viewmodel.ReadingTaskResultUI
import com.example.recognicam.presentation.viewmodel.WorkingMemoryTaskResultUI
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FullAssessmentResultScreen(
    result: FullAssessmentResult,
    onBackToHome: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Animation for score display
    var showScores by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
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
            text = "Full Assessment Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF7FBF7F), // Light green color for header
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Format date for the report
        val dateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val currentDate = dateFormatter.format(Date())

        Text(
            text = "Comprehensive analysis completed on $currentDate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Primary scores with animation
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
                ScoreCircle(
                    score = result.overallScore,
                    label = "ADHD Probability",
                    color = assessmentScoreColor(result.overallScore),
                    size = 120.dp
                )

                ScoreCircle(
                    score = result.confidenceLevel,
                    label = "Confidence",
                    color = Success, // Always green for confidence
                    size = 120.dp
                )
            }
        }

        // Main summary card
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1000, delayMillis = 200))
        ) {
            AssessmentSummaryCard(result)
        }

        // Domain scores with detailed descriptions
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 300))
        ) {
            DomainAnalysisCard(result)
        }

        // Task-by-task performance summary
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 400))
        ) {
            TaskPerformanceSummaryCard(result)
        }

        // Cross-task pattern analysis
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 500))
        ) {
            CrossTaskAnalysisCard(result)
        }

        // Behavioral markers organized by category
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 600))
        ) {
            BehavioralMarkersCard(result)
        }

        // Sensor metrics with interpretations
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 700))
        ) {
            BehaviorAnalysisCard(result)
        }

        // Detailed interpretation and recommendations
        AnimatedVisibility(
            visible = showScores,
            enter = fadeIn(animationSpec = tween(1200, delayMillis = 800))
        ) {
            InterpretationCard(result)
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
fun AssessmentSummaryCard(result: FullAssessmentResult) {
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
                text = "Assessment Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = getScoreText(result.overallScore),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = getDetailedInterpretationText(result),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assessment Quality:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = when {
                        result.confidenceLevel >= 80 -> "Excellent"
                        result.confidenceLevel >= 65 -> "Good"
                        result.confidenceLevel >= 50 -> "Adequate"
                        else -> "Limited"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Success // Always green for assessment quality
                )
            }

            Text(
                text = "This comprehensive assessment combines results from multiple cognitive tasks, analyzing patterns across attention, inhibition, working memory, and cognitive flexibility domains.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainAnalysisCard(result: FullAssessmentResult) {
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

            Text(
                text = getDomainSummary(result),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Inattention score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Inattention",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "${result.attentionScore}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Inattention bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(result.attentionScore / 100f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Error) // Red for inattention
                )
            }

            Text(
                text = getAttentionDescription(result.attentionScore),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hyperactivity score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hyperactivity",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "${result.hyperactivityScore}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hyperactivity bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(result.hyperactivityScore / 100f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Info) // Blue for hyperactivity
                )
            }

            Text(
                text = getHyperactivityDescription(result.hyperactivityScore),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Impulsivity score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Impulsivity",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "${result.impulsivityScore}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Impulsivity bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(result.impulsivityScore / 100f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Warning) // Orange/yellow for impulsivity
                )
            }

            Text(
                text = getImpulsivityDescription(result.impulsivityScore),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPerformanceSummaryCard(result: FullAssessmentResult) {
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
                text = "Task Performance Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Performance across the five cognitive assessment tasks:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Individual task result summaries - now using actual data from task results
            TaskSummaryItem(
                taskName = "Continuous Performance Test",
                summary = "Sustained attention task: ${getCPTPerformanceSummary(getActualTaskScore(result, "CPT"))}",
                score = getActualTaskScore(result, "CPT")
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            TaskSummaryItem(
                taskName = "Reading Assessment",
                summary = "Reading comprehension: ${getReadingPerformanceSummary(getActualTaskScore(result, "Reading"))}",
                score = getActualTaskScore(result, "Reading")
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            TaskSummaryItem(
                taskName = "Go/No-Go Task",
                summary = "Impulse control task: ${getGoNoGoPerformanceSummary(getActualTaskScore(result, "GoNoGo"))}",
                score = getActualTaskScore(result, "GoNoGo")
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            TaskSummaryItem(
                taskName = "Working Memory Task",
                summary = "Working memory task: ${getWorkingMemoryPerformanceSummary(getActualTaskScore(result, "WorkingMemory"))}",
                score = getActualTaskScore(result, "WorkingMemory")
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            TaskSummaryItem(
                taskName = "Attention Shifting Task",
                summary = "Cognitive flexibility task: ${getAttentionShiftingPerformanceSummary(getActualTaskScore(result, "AttentionShifting"))}",
                score = getActualTaskScore(result, "AttentionShifting")
            )
        }
    }
}

@Composable
fun TaskSummaryItem(taskName: String, summary: String, score: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = taskName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "$score%",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = assessmentScoreColor(score)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossTaskAnalysisCard(result: FullAssessmentResult) {
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
                text = "Cross-Task Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Patterns identified across multiple cognitive domains:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Using actual result data for the analysis
            // Response consistency pattern
            PatternAnalysisItem(
                "Response Consistency",
                getResponseConsistencySignificance(result),
                getResponseConsistencyAnalysis(result)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Attention sustainability pattern
            PatternAnalysisItem(
                "Attention Sustainability",
                getAttentionSustainabilitySignificance(result),
                getAttentionSustainabilityAnalysis(result)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cognitive flexibility pattern
            PatternAnalysisItem(
                "Cognitive Flexibility",
                getCognitiveFlexibilitySignificance(result),
                getCognitiveFlexibilityAnalysis(result)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hyperactive behavior pattern
            PatternAnalysisItem(
                "Hyperactive Behavior",
                getHyperactivitySignificance(result),
                getHyperactivityAnalysis(result)
            )
        }
    }
}

@Composable
fun PatternAnalysisItem(title: String, significance: Int, description: String) {
    val significanceColor = when (significance) {
        3 -> Error      // High significance - red
        2 -> Warning    // Moderate significance - orange/yellow
        1 -> Info       // Mild significance - blue
        else -> Success // Normal/no significance - green
    }

    val significanceText = when (significance) {
        3 -> "High"
        2 -> "Moderate"
        1 -> "Mild"
        else -> "Normal"
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator color dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(significanceColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = significanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = significanceColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehavioralMarkersCard(result: FullAssessmentResult) {
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

            Text(
                text = "The most significant behavioral patterns detected across all assessment tasks:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Group markers by domain for better organization and coloring
            val attentionMarkers = result.behavioralMarkers.filter {
                it.name in listOf("Look Away Rate", "Sustained Attention", "Look Away Duration",
                    "Attention Lapses", "Distractibility", "Response Time", "Task Accuracy")
            }

            val hyperactivityMarkers = result.behavioralMarkers.filter {
                it.name in listOf("Fidgeting Score", "Direction Changes", "Restlessness",
                    "Facial Movement", "Blink Rate", "Face Visibility")
            }

            val impulsivityMarkers = result.behavioralMarkers.filter {
                it.name in listOf("Sudden Movements", "Emotion Changes", "Emotion Variability",
                    "Response Variability", "Missed Responses")
            }

            // Other markers
            val otherMarkers = result.behavioralMarkers.filterNot {
                it.name in (attentionMarkers + hyperactivityMarkers + impulsivityMarkers).map { m -> m.name }
            }

            // Only show sections with markers
            if (attentionMarkers.isNotEmpty()) {
                Text(
                    text = "Attention Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                attentionMarkers.forEach { marker ->
                    // Use Error/red color for all attention markers
                    MarkerItemWithDomainColor(marker, Error)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Display hyperactivity markers
            if (hyperactivityMarkers.isNotEmpty()) {
                Text(
                    text = "Hyperactivity Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                hyperactivityMarkers.forEach { marker ->
                    // Use Info/blue color for all hyperactivity markers
                    MarkerItemWithDomainColor(marker, Info)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Display impulsivity markers
            if (impulsivityMarkers.isNotEmpty()) {
                Text(
                    text = "Impulsivity Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                impulsivityMarkers.forEach { marker ->
                    // Use Warning/orange color for all impulsivity markers
                    MarkerItemWithDomainColor(marker, Warning)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Display other markers
            if (otherMarkers.isNotEmpty()) {
                Text(
                    text = "Other Significant Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                otherMarkers.forEach { marker ->
                    // Use gray for other markers
                    MarkerItemWithDomainColor(marker, MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // If no markers at all, show a message
            if (result.behavioralMarkers.isEmpty()) {
                Text(
                    text = "No significant behavioral markers were detected across the tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MarkerItemWithDomainColor(marker: BehavioralMarker, domainColor: Color) {
    // Determine level text based on value relative to threshold
    val levelText = when {
        marker.value > marker.threshold * 1.5f -> "High"
        marker.value > marker.threshold -> "Moderate"
        marker.value > marker.threshold * 0.5f -> "Normal"
        else -> "Low"
    }

    // Determine if higher values are better or worse
    val isHigherBetter = marker.name in listOf("Sustained Attention", "Face Visibility", "Task Accuracy")

    // Format value display
    val displayValue = if (marker.value >= 100f) {
        marker.value.toInt().toString()
    } else {
        // Show 1 decimal place if not a whole number
        String.format("%.1f", marker.value)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Marker color dot - using domain color
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(domainColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = marker.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Show progress bar with more helpful coloring based on better/worse
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        ) {
            // Calculate fill percentage (0-100%)
            val fillPercentage = (marker.value / (marker.threshold * 2f)).coerceIn(0f, 1f)

            // Choose color based on whether high values are better
            val barColor = if (isHigherBetter) {
                // For metrics where high is good (like sustained attention)
                when {
                    marker.value > marker.threshold * 1.2f -> Success // High is good
                    marker.value > marker.threshold * 0.8f -> Warning // Moderate
                    else -> Error // Low is bad
                }
            } else {
                // For metrics where high is bad (like distractibility)
                when {
                    marker.value > marker.threshold * 1.2f -> Error // High is bad
                    marker.value > marker.threshold * 0.8f -> Warning // Moderate
                    else -> Success // Low is good
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillPercentage)
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Value and level
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = levelText,
                style = MaterialTheme.typography.bodySmall,
                color = domainColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehaviorAnalysisCard(result: FullAssessmentResult) {
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
                text = "Behavior Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Analysis from facial expressions, attention patterns, and physical movement during all tasks:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Face metrics section
            Text(
                text = "Attention & Focus Patterns",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BehaviorMetricItem(
                    value = "${result.faceMetrics.lookAwayCount}",
                    label = "Look Aways",
                    description = getLookAwayInterpretation(result.faceMetrics.lookAwayCount),
                    modifier = Modifier.weight(1f)
                )

                BehaviorMetricItem(
                    value = "${result.faceMetrics.distractibilityIndex}%",
                    label = "Distractibility",
                    description = getDistractibilityInterpretation(result.faceMetrics.distractibilityIndex),
                    modifier = Modifier.weight(1f)
                )

                BehaviorMetricItem(
                    value = "${result.faceMetrics.sustainedAttentionScore}%",
                    label = "Attention",
                    description = getSustainedAttentionInterpretation(result.faceMetrics.sustainedAttentionScore),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motion metrics section
            Text(
                text = "Physical Movement Patterns",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BehaviorMetricItem(
                    value = "${result.motionMetrics.fidgetingScore}%",
                    label = "Fidgeting",
                    description = getFidgetingInterpretation(result.motionMetrics.fidgetingScore),
                    modifier = Modifier.weight(1f)
                )

                BehaviorMetricItem(
                    value = "${result.motionMetrics.restlessness}%",
                    label = "Restlessness",
                    description = getRestlessnessInterpretation(result.motionMetrics.restlessness),
                    modifier = Modifier.weight(1f)
                )

                BehaviorMetricItem(
                    value = "${result.motionMetrics.directionChanges}",
                    label = "Movement Changes",
                    description = getDirectionChangesInterpretation(result.motionMetrics.directionChanges),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comprehensive sensor interpretation
            Text(
                text = getComprehensiveSensorInterpretation(result),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BehaviorMetricItem(
    value: String,
    label: String,
    description: String,
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

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpretationCard(result: FullAssessmentResult) {
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
                text = "Assessment Interpretation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = getComprehensiveInterpretation(result),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Recommendations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = getRecommendations(result),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Clinical disclaimer
            Text(
                text = "Note: This assessment is designed as a screening tool and does not constitute a clinical diagnosis. If you have concerns about ADHD, please consult a qualified healthcare professional who can provide a comprehensive evaluation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Helper functions for generating descriptions based on results
private fun getDetailedInterpretationText(result: FullAssessmentResult): String {
    return when {
        result.overallScore >= 70 ->
            "Your assessment shows strong behavioral patterns associated with ADHD across multiple tasks. You demonstrated notable difficulties with sustained attention, impulse control, and consistent performance. These patterns were consistent across different cognitive domains, suggesting they may impact various aspects of daily functioning."

        result.overallScore >= 40 ->
            "Your assessment shows moderate behavioral patterns associated with ADHD across multiple tasks. While some aspects of your performance were typical, you demonstrated notable difficulties in specific areas. The patterns weren't consistent across all domains, which is common in ADHD where symptoms can vary by situation and cognitive demand."

        result.overallScore >= 20 ->
            "Your assessment shows mild behavioral patterns that might be associated with ADHD. Your performance was mostly within typical ranges, with occasional moments of inattention or impulsivity. Most people experience some of these behaviors occasionally, and your results suggest these are mostly within normal variation."

        else ->
            "Your assessment shows very few behaviors associated with ADHD. Your performance demonstrated consistent attention, appropriate response patterns, and typical activity levels throughout the tasks. Your results align closely with what research shows for sustained attention and executive function in individuals without ADHD."
    }
}

private fun getDomainSummary(result: FullAssessmentResult): String {
    val dominantDomain = when {
        result.attentionScore >= result.hyperactivityScore && result.attentionScore >= result.impulsivityScore ->
            "inattention"
        result.hyperactivityScore >= result.attentionScore && result.hyperactivityScore >= result.impulsivityScore ->
            "hyperactivity"
        else ->
            "impulsivity"
    }

    return when (dominantDomain) {
        "inattention" -> "Your assessment indicates that inattention is the most prominent pattern. This profile is consistent with the predominantly inattentive presentation of ADHD, characterized by difficulty sustaining attention, following instructions, and completing tasks that require sustained mental effort."
        "hyperactivity" -> "Your assessment indicates that hyperactivity is the most prominent pattern. This profile shows more physical restlessness and fidgeting than difficulties with attention or impulse control, which aligns with the hyperactive-impulsive presentation of ADHD."
        else -> "Your assessment indicates that impulsivity is the most prominent pattern. This suggests a tendency to act without thinking, difficulty waiting your turn, and interrupting others, which are key characteristics of the hyperactive-impulsive presentation of ADHD."
    }
}

private fun getAttentionDescription(score: Int): String {
    return when {
        score >= 70 -> "Significant difficulty maintaining focus on tasks. Frequent attention lapses and distraction."
        score >= 40 -> "Moderate attention challenges. Some difficulty staying focused for extended periods."
        score >= 20 -> "Mild attention issues. Generally able to focus but with occasional lapses."
        else -> "Typical attention patterns. Able to maintain focus consistently across tasks."
    }
}

private fun getHyperactivityDescription(score: Int): String {
    return when {
        score >= 70 -> "Significant physical restlessness and fidgeting throughout tasks. Difficulty staying physically calm."
        score >= 40 -> "Moderate hyperactivity. Notable fidgeting and movement during tasks requiring stillness."
        score >= 20 -> "Mild physical restlessness. Occasional fidgeting that doesn't significantly impact performance."
        else -> "Typical movement patterns. Appropriate physical stillness during tasks."
    }
}

private fun getImpulsivityDescription(score: Int): String {
    return when {
        score >= 70 -> "Significant difficulty with response inhibition. Frequent impulsive reactions without thinking."
        score >= 40 -> "Moderate impulsivity. Some difficulty waiting and tendency to respond without full consideration."
        score >= 20 -> "Mild impulsivity. Generally able to inhibit responses with occasional impulsive actions."
        else -> "Typical impulse control. Able to appropriately inhibit responses across tasks."
    }
}

// This function now uses the actual task scores from the results
private fun getActualTaskScore(result: FullAssessmentResult, taskType: String): Int {
    return when (taskType) {
        "CPT" -> {
            val cptResult = result.taskResults["CPT"]
            if (cptResult is CPTTaskResult) {
                cptResult.adhdAssessment.adhdProbabilityScore
            } else 0
        }
        "Reading" -> {
            val readingResult = result.taskResults["Reading"]
            if (readingResult is ReadingTaskResultUI) {
                readingResult.adhdAssessment.adhdProbabilityScore
            } else 0
        }
        "GoNoGo" -> {
            val goNoGoResult = result.taskResults["GoNoGo"]
            if (goNoGoResult is GoNoGoTaskResultUI) {
                goNoGoResult.adhdAssessment.adhdProbabilityScore
            } else 0
        }
        "WorkingMemory" -> {
            val workingMemoryResult = result.taskResults["WorkingMemory"]
            if (workingMemoryResult is WorkingMemoryTaskResultUI) {
                workingMemoryResult.adhdAssessment.adhdProbabilityScore
            } else 0
        }
        "AttentionShifting" -> {
            val attentionShiftingResult = result.taskResults["AttentionShifting"]
            if (attentionShiftingResult is AttentionShiftingTaskResultUI) {
                attentionShiftingResult.adhdAssessment.adhdProbabilityScore
            } else 0
        }
        else -> 0
    }
}

private fun getCPTPerformanceSummary(score: Int): String {
    return when {
        score >= 70 -> "Significant difficulty maintaining attention for extended periods. Frequent missed targets and slow responses."
        score >= 40 -> "Moderate attentional challenges. Some missed targets and variable response times."
        score >= 20 -> "Mild attention variability. Generally good performance with occasional lapses."
        else -> "Strong sustained attention. Consistent performance throughout the task."
    }
}

private fun getReadingPerformanceSummary(score: Int): String {
    return when {
        score >= 70 -> "Significant difficulty maintaining focus during reading. Poor comprehension and frequent attention lapses."
        score >= 40 -> "Moderate challenges with reading attention. Some comprehension issues and distractibility."
        score >= 20 -> "Mild reading focus variations. Generally adequate comprehension with occasional attention shifts."
        else -> "Strong reading focus. Good comprehension and sustained attention throughout."
    }
}

private fun getGoNoGoPerformanceSummary(score: Int): String {
    return when {
        score >= 70 -> "Significant impulse control difficulties. Frequent incorrect responses to no-go stimuli."
        score >= 40 -> "Moderate inhibition challenges. Some difficulty stopping responses when required."
        score >= 20 -> "Mild inhibition variations. Generally good impulse control with occasional errors."
        else -> "Strong impulse control. Consistently able to inhibit responses appropriately."
    }
}

private fun getWorkingMemoryPerformanceSummary(score: Int): String {
    return when {
        score >= 70 -> "Significant working memory difficulties. Frequent errors in remembering and processing information."
        score >= 40 -> "Moderate working memory challenges. Some difficulty holding information in mind."
        score >= 20 -> "Mild working memory variations. Generally adequate with occasional lapses."
        else -> "Strong working memory function. Consistently able to hold and manipulate information."
    }
}

private fun getAttentionShiftingPerformanceSummary(score: Int): String {
    return when {
        score >= 70 -> "Significant cognitive flexibility difficulties. Slow adaptation to changing rules and frequent errors."
        score >= 40 -> "Moderate flexibility challenges. Some difficulty switching between tasks and rules."
        score >= 20 -> "Mild flexibility variations. Generally adequate adaptation with occasional delays."
        else -> "Strong cognitive flexibility. Consistently able to adapt to changing rules and requirements."
    }
}

// Cross-task pattern analysis functions - now using actual result data
private fun getResponseConsistencyAnalysis(result: FullAssessmentResult): String {
    val variabilityLevel = (result.attentionScore * 0.3 + result.impulsivityScore * 0.7).toInt()

    return when {
        variabilityLevel >= 70 -> "High inconsistency in response patterns. Your performance showed significant variability in speed and accuracy, particularly when tasks became more demanding. This inconsistency strongly suggests challenges in maintaining consistent attention and inhibitory control."
        variabilityLevel >= 40 -> "Moderate inconsistency in response patterns. Your performance showed some variability in speed and accuracy, particularly when tasks became more demanding. This moderate inconsistency suggests some challenges in maintaining consistent attention."
        variabilityLevel >= 20 -> "Mild inconsistency in responses. Your performance was generally consistent with occasional lapses, particularly during longer task periods. This mild variability is common in many individuals."
        else -> "Highly consistent response patterns across all tasks. Your performance maintained steady speed and accuracy throughout the assessment, which indicates strong attentional control."
    }
}

private fun getResponseConsistencySignificance(result: FullAssessmentResult): Int {
    val variabilityLevel = (result.attentionScore * 0.3 + result.impulsivityScore * 0.7).toInt()

    return when {
        variabilityLevel >= 70 -> 3 // High significance
        variabilityLevel >= 40 -> 2 // Moderate significance
        variabilityLevel >= 20 -> 1 // Mild significance
        else -> 0 // No significance
    }
}

private fun getAttentionSustainabilityAnalysis(result: FullAssessmentResult): String {
    return when {
        result.attentionScore >= 70 -> "Significant difficulty sustaining attention across all tasks. Your performance showed frequent attention lapses, particularly as tasks progressed. This consistent pattern of attention difficulties is strongly associated with ADHD."
        result.attentionScore >= 40 -> "Moderate challenges maintaining attention. Your focus was inconsistent across tasks, with more attention lapses during complex or lengthy activities. This pattern suggests some attentional control difficulties."
        result.attentionScore >= 20 -> "Mild attention sustainability issues. Your attention was generally well-maintained with occasional lapses, typically during more demanding portions of tasks. This pattern is within the range of common experience."
        else -> "Strong attention sustainability across all tasks. Your focus remained consistent even during challenging portions of the assessment, indicating robust attentional control."
    }
}

private fun getAttentionSustainabilitySignificance(result: FullAssessmentResult): Int {
    return when {
        result.attentionScore >= 70 -> 3
        result.attentionScore >= 40 -> 2
        result.attentionScore >= 20 -> 1
        else -> 0
    }
}

private fun getCognitiveFlexibilityAnalysis(result: FullAssessmentResult): String {
    // Use actual Attention Shifting task score instead of hard-coded value
    val attentionShiftingScore = getActualTaskScore(result, "AttentionShifting")

    val flexibility = (result.attentionScore * 0.4 + result.impulsivityScore * 0.2 +
            attentionShiftingScore * 0.4).toInt()

    return when {
        flexibility >= 70 -> "Significant challenges with cognitive flexibility. You had considerable difficulty adapting to changing rules and requirements across tasks. This pattern of inflexible thinking is often seen in ADHD, particularly when combined with impulsivity."
        flexibility >= 40 -> "Moderate cognitive flexibility issues. You showed some difficulty switching between different task requirements and adapting to new rules. This suggests moderate challenges with executive function."
        flexibility >= 20 -> "Mild cognitive flexibility variations. You generally adapted well to changing requirements with occasional delays or errors. These mild challenges are common and typically don't impact daily functioning."
        else -> "Strong cognitive flexibility demonstrated across tasks. You readily adapted to changing rules and shifted your approach appropriately, indicating robust executive functioning."
    }
}

private fun getCognitiveFlexibilitySignificance(result: FullAssessmentResult): Int {
    // Use actual Attention Shifting task score instead of hard-coded value
    val attentionShiftingScore = getActualTaskScore(result, "AttentionShifting")

    val flexibility = (result.attentionScore * 0.4 + result.impulsivityScore * 0.2 +
            attentionShiftingScore * 0.4).toInt()

    return when {
        flexibility >= 70 -> 3
        flexibility >= 40 -> 2
        flexibility >= 20 -> 1
        else -> 0
    }
}

private fun getHyperactivityAnalysis(result: FullAssessmentResult): String {
    return when {
        result.hyperactivityScore >= 70 -> "Significant hyperactive behavior detected throughout the assessment. Sensor analysis showed high levels of fidgeting, position changes, and physical restlessness across all tasks. This consistent pattern of physical hyperactivity is strongly associated with ADHD."
        result.hyperactivityScore >= 40 -> "Moderate hyperactive behavior observed. Analysis showed notable fidgeting and movement during tasks requiring sustained attention. This pattern of physical restlessness is often associated with ADHD but can occur in various contexts."
        result.hyperactivityScore >= 20 -> "Mild hyperactive behavior detected. Some fidgeting and minor movement patterns were observed, particularly during more demanding tasks. This level of movement is common and typically doesn't impact performance."
        else -> "Minimal hyperactive behavior. You maintained appropriate physical stillness throughout the assessment, with movement patterns well within typical ranges."
    }
}

private fun getHyperactivitySignificance(result: FullAssessmentResult): Int {
    return when {
        result.hyperactivityScore >= 70 -> 3
        result.hyperactivityScore >= 40 -> 2
        result.hyperactivityScore >= 20 -> 1
        else -> 0
    }
}

// Sensor metrics interpretation functions
private fun getLookAwayInterpretation(count: Int): String {
    return when {
        count > 15 -> "High frequency (significant)"
        count > 8 -> "Moderate frequency"
        count > 4 -> "Occasional (typical)"
        else -> "Minimal (focused)"
    }
}

private fun getDistractibilityInterpretation(score: Int): String {
    return when {
        score > 70 -> "High (significant)"
        score > 40 -> "Moderate"
        score > 20 -> "Mild"
        else -> "Minimal (focused)"
    }
}

private fun getSustainedAttentionInterpretation(score: Int): String {
    return when {
        score < 30 -> "Very low (significant)"
        score < 50 -> "Below average"
        score < 70 -> "Average"
        else -> "Strong (focused)"
    }
}

private fun getFidgetingInterpretation(score: Int): String {
    return when {
        score > 70 -> "Very high (significant)"
        score > 50 -> "Above average"
        score > 30 -> "Average"
        else -> "Minimal"
    }
}

private fun getRestlessnessInterpretation(score: Int): String {
    return when {
        score > 70 -> "Very high (significant)"
        score > 50 -> "Above average"
        score > 30 -> "Average"
        else -> "Minimal"
    }
}

private fun getDirectionChangesInterpretation(changes: Int): String {
    return when {
        changes > 60 -> "Very frequent (significant)"
        changes > 40 -> "Frequent"
        changes > 20 -> "Occasional"
        else -> "Minimal"
    }
}

private fun getComprehensiveSensorInterpretation(result: FullAssessmentResult): String {
    // Check for attention and hyperactivity issues based on metrics
    val attentionIssues = result.faceMetrics.lookAwayCount > 10 ||
            result.faceMetrics.distractibilityIndex > 60 ||
            result.faceMetrics.sustainedAttentionScore < 40

    val hyperactivityIssues = result.motionMetrics.fidgetingScore > 60 ||
            result.motionMetrics.restlessness > 60 ||
            result.motionMetrics.directionChanges > 50

    return when {
        attentionIssues && hyperactivityIssues ->
            "Your behavioral analysis shows significant patterns associated with both attentional difficulties and hyperactivity. These patterns were consistent across multiple tasks and suggest notable challenges with sustained attention and physical restlessness."

        attentionIssues ->
            "Your behavioral analysis primarily shows attention-related patterns. Sensor data indicates difficulties maintaining visual focus on tasks, with frequent attention shifts and distractibility. Physical movement was less pronounced than attention difficulties."

        hyperactivityIssues ->
            "Your behavioral analysis primarily shows hyperactivity-related patterns. Sensor data indicates significant physical restlessness and fidgeting throughout tasks, though your visual attention was relatively well-maintained."

        else ->
            "Your behavioral analysis shows patterns within typical ranges. Sensor data indicates appropriate sustained attention with minimal distractibility, and physical movement was within normal limits during tasks."
    }
}

// Detailed interpretation and recommendations
private fun getComprehensiveInterpretation(result: FullAssessmentResult): String {
    return when {
        result.overallScore >= 70 ->
            "Your comprehensive assessment results show consistent patterns strongly associated with ADHD across multiple cognitive domains. You demonstrated significant challenges with sustained attention, response inhibition, and maintaining consistent performance. These patterns were evident throughout different types of tasks, suggesting they may impact various aspects of daily functioning.\n\n" +
                    "Your performance showed particular difficulty with ${getPrimaryDifficulty(result)}, which is a core characteristic of ADHD. The consistency of these patterns across diverse cognitive tasks strengthens the reliability of these findings."

        result.overallScore >= 40 ->
            "Your comprehensive assessment results show moderate patterns associated with ADHD. While you performed well in some areas, you demonstrated challenges with ${getPrimaryDifficulty(result)}. These patterns weren't equally present across all tasks, which is common in ADHD where symptoms can vary by context and task demands.\n\n" +
                    "The moderate nature of these findings suggests you may experience some functional impact in daily activities requiring sustained attention or behavioral control, though you likely have developed some compensation strategies."

        result.overallScore >= 20 ->
            "Your comprehensive assessment results show mild patterns that could be associated with ADHD. Your performance was largely within typical ranges, with occasional challenges in ${getPrimaryDifficulty(result)}. These mild variations are common in the general population and may not necessarily indicate ADHD.\n\n" +
                    "The assessment suggests you generally function well across most cognitive domains, with specific situations potentially presenting more challenge than others."

        else ->
            "Your comprehensive assessment results show very few patterns associated with ADHD. Your performance demonstrated consistent attention, appropriate response control, and typical activity levels throughout all tasks. Your results align closely with typical cognitive function across attention, inhibition, working memory, and cognitive flexibility domains.\n\n" +
                    "The assessment indicates robust cognitive function with performance well within typical ranges across all tasks."
    }
}

private fun getPrimaryDifficulty(result: FullAssessmentResult): String {
    return when {
        result.attentionScore >= result.hyperactivityScore && result.attentionScore >= result.impulsivityScore ->
            "sustained attention and focus"
        result.hyperactivityScore >= result.attentionScore && result.hyperactivityScore >= result.impulsivityScore ->
            "physical restlessness and hyperactivity"
        else ->
            "impulse control and response inhibition"
    }
}

private fun getRecommendations(result: FullAssessmentResult): String {
    return when {
        result.overallScore >= 70 ->
            "• Consider consulting with a healthcare professional for a comprehensive clinical evaluation of ADHD\n" +
                    "• Discuss potential treatment options, which may include behavioral strategies, environmental modifications, or medication\n" +
                    "• Consider exploring structured organizational systems and tools designed for ADHD management\n" +
                    "• Regular physical exercise and mindfulness practices may help manage symptoms\n" +
                    "• Learning specific strategies for your most challenging areas may improve daily functioning"

        result.overallScore >= 40 ->
            "• Consider discussing these results with a healthcare professional to determine if further evaluation is warranted\n" +
                    "• Explore behavioral strategies that target your specific areas of challenge\n" +
                    "• Environmental modifications like reducing distractions may improve performance\n" +
                    "• Regular physical exercise and mindfulness practices may be beneficial\n" +
                    "• Consider learning more about attention management techniques"

        result.overallScore >= 20 ->
            "• These mild patterns may benefit from simple organizational and attention strategies\n" +
                    "• Consider discussing these results with a healthcare professional if you experience functional challenges\n" +
                    "• Basic mindfulness and attention training may be helpful for optimizing focus\n" +
                    "• Regular sleep, exercise, and stress management can improve cognitive performance\n" +
                    "• Monitor whether these patterns impact important areas of functioning"

        else ->
            "• Your assessment shows strong cognitive function across all domains\n" +
                    "• Continue any current strategies that support your cognitive health\n" +
                    "• Regular physical activity, adequate sleep, and stress management support optimal cognitive function\n" +
                    "• No specific interventions for ADHD appear necessary based on this assessment"
    }
}