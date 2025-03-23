
package com.example.recognicam.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.recognicam.presentation.viewmodel.Rule
import kotlinx.coroutines.delay

@Composable
fun RuleChangeOverlay(
    isVisible: Boolean,
    newRule: Rule,
    onAnimationComplete: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(
            animationSpec = tween(300),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
            animationSpec = tween(300),
            shrinkTowards = Alignment.Top
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 70.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Simple alert icon
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.titleLarge
                )

                // New rule in a compact format
                val ruleText = when (newRule) {
                    Rule.COLOR -> "Now tap BLUE shapes"
                    Rule.SHAPE -> "Now tap SQUARE shapes"
                }

                Text(
                    text = ruleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    // Dismiss overlay after a shorter delay
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2000) // Show for 2 seconds
            onAnimationComplete()
        }
    }
}