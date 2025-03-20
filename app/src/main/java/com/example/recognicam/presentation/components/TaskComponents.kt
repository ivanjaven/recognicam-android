package com.example.recognicam.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recognicam.data.analysis.BehavioralMarker
import com.example.recognicam.presentation.theme.Error
import com.example.recognicam.presentation.theme.Success
import com.example.recognicam.presentation.theme.Warning

/**
 * Common UI components used throughout the application
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentTaskCard(
    title: String,
    description: String,
    duration: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with gradient
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Task information with improved typography
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use emoji instead of material icon
                    Text(
                        text = "⏱️",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Text(
                        text = duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Use emoji instead of material icon
            Text(
                text = "➡️",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun ScoreCircle(
    score: Int,
    label: String,
    color: Color,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    animationDuration: Int = 1000
) {
    // Capture theme colors in the Composable context
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(animationDuration))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .padding(strokeWidth / 2),
                contentAlignment = Alignment.Center
            ) {
                // Background circle with captured backgroundColor
                Canvas(modifier = Modifier.size(size)) {
                    drawArc(
                        color = backgroundColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Score arc
                Canvas(modifier = Modifier.size(size)) {
                    val sweepAngle = (score / 100f) * 360f
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center score
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
    // Capture theme colors before using in Canvas
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "$score%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Updated score bar with stroke style similar to the pie chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            // Draw background track using captured backgroundColor
            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(7.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw filled progress with slight inset
            if (score > 0) {
                val width = (size.width * (score / 100f)) - 4.dp.toPx()
                if (width > 0) {
                    drawRoundRect(
                        color = color,
                        cornerRadius = CornerRadius(7.dp.toPx()),
                        size = Size(width, size.height - 4.dp.toPx()),
                        topLeft = Offset(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }

        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BehavioralMarkerItem(
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
            percentValue > 75 -> Success  // Green (good)
            percentValue > 50 -> Warning  // Orange (moderate)
            else -> Error                 // Red (concerning)
        }
    } else {
        // For metrics where high values are BAD (like distractibility)
        when {
            percentValue > 75 -> Error    // Red (concerning)
            percentValue > 50 -> Warning  // Orange (moderate)
            else -> Success               // Green (good)
        }
    }

    // Round value to 2 decimal places
    val displayValue = if (marker.value >= 100) {
        "${marker.value.toInt()}"
    } else if (marker.value == marker.value.toInt().toFloat()) {
        "${marker.value.toInt()}"
    } else {
        String.format("%.1f", marker.value) // Show only 1 decimal place
    }

    // Light gray background for unfilled part (same as domain bars)
    val barBackgroundColor = Color(0xFFEEEEEE)
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onExpandToggle
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(markerColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = marker.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar with light gray background and colored fill portion
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barBackgroundColor) // Light gray for unfilled part
                    ) {
                        // Only show the filled portion if there's a value
                        if (percentValue > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentValue / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(markerColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Value with interpretation text
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )

                    Text(
                        text = if (isHighValueGood) {
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
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = markerColor,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Expanded description
            AnimatedVisibility(visible = isExpanded) {
                val description = marker.description.ifEmpty {
                    com.example.recognicam.core.utils.getMetricDescription(marker.name)
                }

                // If description is long, make it more concise
                val conciseDescription = if (description.length > 100) {
                    description.split(".").firstOrNull() ?: description
                } else {
                    description
                }

                Text(
                    text = conciseDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantColor,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 4.dp)
                )
            }
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
    // Format number as needed - remove .0 if applicable
    val displayValue = if (value.endsWith(".0")) {
        value.substring(0, value.length - 2)
    } else {
        value
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayValue,
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
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun assessmentScoreColor(score: Int): Color {
    return when {
        score >= 70 -> Error      // High (red)
        score >= 40 -> Warning    // Medium (orange)
        else -> Success           // Low (green)
    }
}