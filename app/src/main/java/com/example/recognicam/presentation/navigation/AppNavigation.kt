package com.example.recognicam.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recognicam.presentation.screens.*
import com.example.recognicam.presentation.viewmodel.ResultsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("cpt_task") {
            CPTTaskScreen(navController)
        }
        composable("reading_task") {
            ReadingTaskScreen(navController)
        }
        composable("go_no_go_task") {
            GoNoGoTaskScreen(navController)
        }
        composable("working_memory_task") {
            WorkingMemoryTaskScreen(navController)
        }
        composable("attention_shifting_task") {
            AttentionShiftingTaskScreen(navController)
        }
        composable(
            "results/{taskType}",
            arguments = listOf(navArgument("taskType") { type = NavType.StringType })
        ) {
            val taskType = it.arguments?.getString("taskType") ?: ""
            val resultsViewModel: ResultsViewModel = viewModel(
                factory = ResultsViewModel.Factory(taskType)
            )
            ResultsScreen(navController, taskType, resultsViewModel)
        }
    }
}