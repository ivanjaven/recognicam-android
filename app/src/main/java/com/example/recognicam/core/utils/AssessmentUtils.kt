package com.example.recognicam.core.utils

/**
 * Shared utilities for assessment interpretation
 */

fun getInterpretationText(score: Int): String {
    return when {
        score >= 70 ->
            "The assessment detected a high frequency of behaviors strongly associated with ADHD. " +
                    "Analysis of your task performance, facial attention patterns, and movement metrics all indicate significant " +
                    "markers consistent with ADHD symptoms. This includes elevated levels of inattention, hyperactivity, and " +
                    "impulsivity compared to typical patterns."

        score >= 40 ->
            "The assessment detected a moderate level of behaviors that may be associated with ADHD. " +
                    "While not conclusive, your performance shows some characteristic patterns in attention shifts, " +
                    "response variability, and movement that align with ADHD indicators. Consider discussing these " +
                    "results with a healthcare professional if you experience related challenges in daily life."

        score > 20 ->
            "The assessment detected some behaviors that can be associated with ADHD, though at levels " +
                    "that are also common in the general population. Your performance showed mostly typical patterns " +
                    "with some occasional variations in attention or movement. The observed patterns are not strongly " +
                    "indicative of ADHD."

        else ->
            "The assessment detected few or no behaviors typically associated with ADHD. " +
                    "Your performance showed consistent attention, appropriate response patterns, and typical " +
                    "movement levels throughout the task. These patterns align closely with what research " +
                    "shows for sustained attention in individuals without ADHD."
    }
}

fun getScoreText(score: Int): String {
    return when {
        score >= 70 -> "High likelihood of ADHD-related behavior patterns"
        score >= 40 -> "Moderate indication of ADHD-related behavior patterns"
        score > 20 -> "Some ADHD-related behaviors detected"
        else -> "Few ADHD-related behaviors detected"
    }
}