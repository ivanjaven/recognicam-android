// FullAssessmentResultScreen.kt
package com.example.recognicam.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.recognicam.presentation.components.*
import com.example.recognicam.presentation.viewmodel.FullAssessmentResult
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
            text = "Completed on $currentDate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Primary scores with animation
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

        // Explanation Card
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
                    text = "Understanding Your Results",
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
                    text = getInterpretationText(result.overallScore),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Note: This comprehensive assessment combines data from multiple cognitive tasks, but is not a clinical diagnosis.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Domain scores
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
                    score = result.attentionScore,
                    label = "Inattention",
                    color = com.example.recognicam.presentation.theme.Error,
                    description = "Difficulty maintaining focus and completing tasks without distraction"
                )

                Spacer(modifier = Modifier.height(12.dp))

                DomainScoreBar(
                    score = result.hyperactivityScore,
                    label = "Hyperactivity",
                    color = com.example.recognicam.presentation.theme.Info,
                    description = "Excessive movement, fidgeting, and physical restlessness"
                )

                Spacer(modifier = Modifier.height(12.dp))

                DomainScoreBar(
                    score = result.impulsivityScore,
                    label = "Impulsivity",
                    color = com.example.recognicam.presentation.theme.Warning,
                    description = "Acting without thinking of consequences, difficulty waiting"
                )
            }
        }

        // Behavioral markers
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

                var expandedMarker by remember { mutableStateOf<String?>(null) }

                // Display top 5 most significant markers
                result.behavioralMarkers.take(5).forEach { marker ->
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

        // Sensor metrics
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
                    text = "Attention & Movement Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Data collected from facial analysis and movement during tasks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Face metrics - highlighted metrics
                Text(
                    text = "Attention Patterns",
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
                        description = "Total count",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.faceMetrics.sustainedAttentionScore}%",
                        label = "Sustained Focus",
                        description = "Attention level",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.faceMetrics.distractibilityIndex}%",
                        label = "Distractibility",
                        description = "Distraction level",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Motion metrics - highlighted metrics
                Text(
                    text = "Movement Patterns",
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
                        description = "Small movements",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.motionMetrics.restlessness}%",
                        label = "Restlessness",
                        description = "Overall movement",
                        modifier = Modifier.weight(1f)
                    )

                    ResultMetricItem(
                        value = "${result.motionMetrics.directionChanges}",
                        label = "Direction Changes",
                        description = "Movement shifts",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Back button
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Back to Home",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}