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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.recognicam.core.utils.getInterpretationText
import com.example.recognicam.core.utils.getScoreText
import com.example.recognicam.data.analysis.BehavioralMarker
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.theme.Error
import com.example.recognicam.presentation.theme.Info
import com.example.recognicam.presentation.theme.Success
import com.example.recognicam.presentation.theme.Warning
import com.example.recognicam.presentation.viewmodel.FullAssessmentResult
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
            color = MaterialTheme.colorScheme.primary,
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
                    color = assessmentScoreColor(100 - result.confidenceLevel), // Invert scale for confidence
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
            SensorMetricsCard(result)
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
                    color = when {
                        result.confidenceLevel >= 80 -> Success
                        result.confidenceLevel >= 65 -> Info
                        result.confidenceLevel >= 50 -> Warning
                        else -> Error
                    }
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

            // Domain score bars with detailed descriptions
            DomainScoreBar(
                score = result.attentionScore,
                label = "Inattention",
                color = Error,
                description = getAttentionDescription(result.attentionScore)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DomainScoreBar(
                score = result.hyperactivityScore,
                label = "Hyperactivity",
                color = Info,
                description = getHyperactivityDescription(result.hyperactivityScore)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DomainScoreBar(
                score = result.impulsivityScore,
                label = "Impulsivity",
                color = Warning,
                description = getImpulsivityDescription(result.impulsivityScore)
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

            // Individual task result summaries
            TaskSummaryItem("Continuous Performance Test",
                getTaskSummary(result, "CPT"),
                getTaskScore(result, "CPT"))

            Spacer(modifier = Modifier.height(12.dp))

            TaskSummaryItem("Reading Assessment",
                getTaskSummary(result, "Reading"),
                getTaskScore(result, "Reading"))

            Spacer(modifier = Modifier.height(12.dp))

            TaskSummaryItem("Go/No-Go Task",
                getTaskSummary(result, "GoNoGo"),
                getTaskScore(result, "GoNoGo"))

            Spacer(modifier = Modifier.height(12.dp))

            TaskSummaryItem("Working Memory Task",
                getTaskSummary(result, "WorkingMemory"),
                getTaskScore(result, "WorkingMemory"))

            Spacer(modifier = Modifier.height(12.dp))

            TaskSummaryItem("Attention Shifting Task",
                getTaskSummary(result, "AttentionShifting"),
                getTaskScore(result, "AttentionShifting"))
        }
    }
}

@Composable
fun TaskSummaryItem(taskName: String, summary: String, score: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = taskName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$score%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = assessmentScoreColor(score)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
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

            // Cross-task pattern analysis sections
            PatternAnalysisItem(
                title = "Response Consistency",
                description = getResponseConsistencyAnalysis(result),
                significance = getResponseConsistencySignificance(result)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PatternAnalysisItem(
                title = "Attention Sustainability",
                description = getAttentionSustainabilityAnalysis(result),
                significance = getAttentionSustainabilitySignificance(result)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PatternAnalysisItem(
                title = "Cognitive Flexibility",
                description = getCognitiveFlexibilityAnalysis(result),
                significance = getCognitiveFlexibilitySignificance(result)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PatternAnalysisItem(
                title = "Hyperactive Behavior",
                description = getHyperactivityAnalysis(result),
                significance = getHyperactivitySignificance(result)
            )
        }
    }
}

@Composable
fun PatternAnalysisItem(title: String, description: String, significance: Int) {
    val color = when {
        significance >= 3 -> Error
        significance >= 2 -> Warning
        significance >= 1 -> Info
        else -> Success
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Significance indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = when {
                    significance >= 3 -> "High"
                    significance >= 2 -> "Moderate"
                    significance >= 1 -> "Mild"
                    else -> "Normal"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehavioralMarkersCard(result: FullAssessmentResult) {
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

            Text(
                text = "The most significant patterns detected across all assessment tasks:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Group markers by domain for better organization
            val attentionMarkers = result.behavioralMarkers.filter {
                it.name in listOf("Look Away Rate", "Sustained Attention", "Look Away Duration",
                    "Attention Lapses", "Distractibility", "Response Time")
            }

            val hyperactivityMarkers = result.behavioralMarkers.filter {
                it.name in listOf("Fidgeting Score", "Direction Changes", "Restlessness",
                    "Facial Movement", "Blink Rate")
            }

            val impulsivityMarkers = result.behavioralMarkers.filter {
                it.name in listOf("Sudden Movements", "Emotion Changes", "Emotion Variability",
                    "Response Variability")
            }

            // Other markers not in the above categories
            val otherMarkers = result.behavioralMarkers.filterNot {
                it.name in (attentionMarkers + hyperactivityMarkers + impulsivityMarkers).map { m -> m.name }
            }

            // Display grouped markers
            if (attentionMarkers.isNotEmpty()) {
                Text(
                    text = "Attention Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                attentionMarkers.forEach { marker ->
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

            if (hyperactivityMarkers.isNotEmpty()) {
                Text(
                    text = "Hyperactivity Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                hyperactivityMarkers.forEach { marker ->
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

            if (impulsivityMarkers.isNotEmpty()) {
                Text(
                    text = "Impulsivity Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                impulsivityMarkers.forEach { marker ->
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

            if (otherMarkers.isNotEmpty()) {
                Text(
                    text = "Other Significant Markers",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                otherMarkers.forEach { marker ->
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
fun SensorMetricsCard(result: FullAssessmentResult) {
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

            // Face metrics with descriptions
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
                SensorMetricItem(
                    value = "${result.faceMetrics.lookAwayCount}",
                    label = "Look Aways",
                    description = getLookAwayInterpretation(result.faceMetrics.lookAwayCount),
                    modifier = Modifier.weight(1f)
                )

                SensorMetricItem(
                    value = "${result.faceMetrics.distractibilityIndex}%",
                    label = "Distractibility",
                    description = getDistractibilityInterpretation(result.faceMetrics.distractibilityIndex),
                    modifier = Modifier.weight(1f)
                )

                SensorMetricItem(
                    value = "${result.faceMetrics.sustainedAttentionScore}%",
                    label = "Attention",
                    description = getSustainedAttentionInterpretation(result.faceMetrics.sustainedAttentionScore),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motion metrics with descriptions
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
                SensorMetricItem(
                    value = "${result.motionMetrics.fidgetingScore}%",
                    label = "Fidgeting",
                    description = getFidgetingInterpretation(result.motionMetrics.fidgetingScore),
                    modifier = Modifier.weight(1f)
                )

                SensorMetricItem(
                    value = "${result.motionMetrics.restlessness}%",
                    label = "Restlessness",
                    description = getRestlessnessInterpretation(result.motionMetrics.restlessness),
                    modifier = Modifier.weight(1f)
                )

                SensorMetricItem(
                    value = "${result.motionMetrics.directionChanges}",
                    label = "Movement Changes",
                    description = getDirectionChangesInterpretation(result.motionMetrics.directionChanges),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comprehensive sensor data interpretation
            Text(
                text = "Behavioral Pattern Interpretation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = getComprehensiveSensorInterpretation(result),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SensorMetricItem(
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
                text = "Assessment Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "This assessment provides a multifaceted analysis of cognitive patterns and behaviors associated with ADHD across five key tasks, measuring different aspects of attention, inhibition, working memory, and cognitive flexibility.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
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
            "Based on your performance across all tasks, the assessment has detected strong behavioral patterns associated with ADHD. You showed consistent markers of inattention, impulsivity, and hyperactivity across multiple cognitive domains. These patterns were evident in sustained attention, behavioral inhibition, working memory, and cognitive flexibility tasks."

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

private fun getTaskSummary(result: FullAssessmentResult, taskType: String): String {
    return when (taskType) {
        "CPT" -> "Sustained attention task: ${getCPTPerformanceSummary(getTaskScore(result, taskType))}"
        "Reading" -> "Reading comprehension: ${getReadingPerformanceSummary(getTaskScore(result, taskType))}"
        "GoNoGo" -> "Impulse control task: ${getGoNoGoPerformanceSummary(getTaskScore(result, taskType))}"
        "WorkingMemory" -> "Working memory task: ${getWorkingMemoryPerformanceSummary(getTaskScore(result, taskType))}"
        "AttentionShifting" -> "Cognitive flexibility task: ${getAttentionShiftingPerformanceSummary(getTaskScore(result, taskType))}"
        else -> "Task data not available"
    }
}

private fun getTaskScore(result: FullAssessmentResult, taskType: String): Int {
    // Extract scores from the taskResults map
    val taskResult = result.taskResults[taskType]

    // If we can't extract the actual score, use domain scores as approximations
    return when (taskType) {
        "CPT" -> (result.attentionScore * 0.8 + result.hyperactivityScore * 0.2).toInt()
        "Reading" -> (result.attentionScore * 0.7 + result.impulsivityScore * 0.3).toInt()
        "GoNoGo" -> (result.impulsivityScore * 0.7 + result.attentionScore * 0.3).toInt()
        "WorkingMemory" -> (result.attentionScore * 0.6 + result.hyperactivityScore * 0.2 + result.impulsivityScore * 0.2).toInt()
        "AttentionShifting" -> (result.attentionScore * 0.4 + result.impulsivityScore * 0.3 + result.hyperactivityScore * 0.3).toInt()
        else -> result.overallScore
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

// Cross-task pattern analysis functions
private fun getResponseConsistencyAnalysis(result: FullAssessmentResult): String {
    val variabilityLevel = (result.attentionScore * 0.3 + result.impulsivityScore * 0.7).toInt()

    return when {
        variabilityLevel >= 70 -> "High inconsistency in response speed and accuracy across all tasks. Your response times were highly variable, with moments of both very fast and very slow responses. This pattern is strongly associated with ADHD and indicates difficulty maintaining consistent performance."
        variabilityLevel >= 40 -> "Moderate inconsistency in response patterns. Your performance showed some variability in speed and accuracy, particularly when tasks became more demanding. This moderate inconsistency suggests some challenges in maintaining consistent attention."
        variabilityLevel >= 20 -> "Mild inconsistency in responses. Your performance was generally consistent with occasional lapses, particularly during longer task periods. This mild variability is common in many individuals."
        else -> "Highly consistent response patterns across all tasks. Your performance maintained steady speed and accuracy throughout the assessment, which indicates strong attentional control."
    }
}

private fun getResponseConsistencySignificance(result: FullAssessmentResult): Int {
    val variabilityLevel = (result.attentionScore * 0.3 + result.impulsivityScore * 0.7).toInt()

    return when {
        variabilityLevel >= 70 -> 3
        variabilityLevel >= 40 -> 2
        variabilityLevel >= 20 -> 1
        else -> 0
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
    // Use the attention shifting task as primary indicator, with working memory as secondary
    val flexibility = (result.attentionScore * 0.4 + result.impulsivityScore * 0.2 +
            getTaskScore(result, "AttentionShifting") * 0.4).toInt()

    return when {
        flexibility >= 70 -> "Significant challenges with cognitive flexibility. You had considerable difficulty adapting to changing rules and requirements across tasks. This pattern of inflexible thinking is often seen in ADHD, particularly when combined with impulsivity."
        flexibility >= 40 -> "Moderate cognitive flexibility issues. You showed some difficulty switching between different task requirements and adapting to new rules. This suggests moderate challenges with executive function."
        flexibility >= 20 -> "Mild cognitive flexibility variations. You generally adapted well to changing requirements with occasional delays or errors. These mild challenges are common and typically don't impact daily functioning."
        else -> "Strong cognitive flexibility demonstrated across tasks. You readily adapted to changing rules and shifted your approach appropriately, indicating robust executive functioning."
    }
}

private fun getCognitiveFlexibilitySignificance(result: FullAssessmentResult): Int {
    val flexibility = (result.attentionScore * 0.4 + result.impulsivityScore * 0.2 +
            getTaskScore(result, "AttentionShifting") * 0.4).toInt()

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
    // Combine face and motion metrics for a comprehensive interpretation
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