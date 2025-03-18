// HomeScreen.kt
package com.example.recognicam.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.recognicam.presentation.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Text(
            text = "ADHD Assessment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Complete these tasks to help screen for ADHD markers",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Task list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TaskCard(
                title = "Continuous Performance Test",
                description = "Measures your ability to sustain attention over time",
                duration = "2 minutes",
                icon = "👁️",
                onClick = { navController.navigate("cpt_task") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TaskCard(
                title = "Reading Assessment",
                description = "Evaluates focus during reading comprehension",
                duration = "3 minutes",
                icon = "📚",
                onClick = { navController.navigate("reading_task") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TaskCard(
                title = "Go/No-Go Task",
                description = "Tests impulse control and inhibition",
                duration = "2 minutes",
                icon = "🚦",
                onClick = { navController.navigate("go_no_go_task") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TaskCard(
                title = "Working Memory Task",
                description = "Assesses memory and cognitive processing",
                duration = "2 minutes",
                icon = "🧠",
                onClick = { navController.navigate("working_memory_task") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TaskCard(
                title = "Attention Shifting Task",
                description = "Measures cognitive flexibility and attention shifting",
                duration = "2 minutes",
                icon = "🔄",
                onClick = { navController.navigate("attention_shifting_task") }
            )
        }

        // Footer disclaimer
        Text(
            text = "This app is for screening purposes only and does not provide a clinical diagnosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}