package com.example.recognicam.core.utils

/**
 * Shared utilities for assessment interpretation
 */

fun getInterpretationText(score: Int): String {
    return when {
        score >= 70 ->
            "The assessment detected behavioral patterns strongly associated with ADHD. " +
                    "Analysis shows significant markers in attention patterns (looking away from task), " +
                    "response consistency, and movement levels. While this is not a diagnosis, these " +
                    "patterns align with clinical ADHD indicators. A professional evaluation is recommended."

        score >= 40 ->
            "The assessment detected moderate behaviors that may be associated with ADHD. " +
                    "These include variations in attention, response timing, and activity level that exceed " +
                    "typical ranges. Remember that everyone shows some of these behaviors occasionally – they only " +
                    "suggest ADHD when they occur frequently and affect daily functioning. Consider " +
                    "discussing these results with a healthcare professional if these patterns cause challenges."

        score >= 20 ->
            "The assessment detected mild behaviors that may be associated with ADHD. " +
                    "Your performance showed mostly typical patterns of attention and response, " +
                    "with some variations that are common in the general population. " +
                    "Everyone experiences moments of distraction or restlessness, and this " +
                    "assessment suggests these are mostly within typical ranges."

        else ->
            "The assessment detected very few behaviors associated with ADHD. " +
                    "Your performance showed consistent attention, appropriate response patterns, " +
                    "and typical activity levels throughout the tasks. These patterns align closely " +
                    "with what research shows for sustained attention in individuals without ADHD."
    }
}

fun getScoreText(score: Int): String {
    return when {
        score >= 70 -> "High likelihood of ADHD-related behavior patterns"
        score >= 40 -> "Moderate indications of ADHD-related behavior patterns"
        score >= 20 -> "Mild indications of ADHD-related behavior patterns"
        else -> "Very few ADHD-related behaviors detected"
    }
}

// Helper function to explain metrics
fun getMetricDescription(metricName: String): String {
    return when (metricName) {
        "Response Time" -> "Average time taken to respond to stimuli. Slower times may indicate processing difficulties."
        "Response Variability" -> "Consistency of response timing. High variability is a key ADHD indicator."
        "Task Accuracy" -> "Percentage of correct responses. Lower accuracy may indicate attention difficulties."
        "Missed Responses" -> "Target stimuli that received no response. May indicate inattention."
        "Look Away Rate" -> "How often attention shifts away from the task. Some looking away is normal."
        "Sustained Attention" -> "Ability to maintain focus over time. Lower scores indicate difficulty maintaining attention."
        "Look Away Duration" -> "How long attention typically stays away from task when distracted."
        "Attention Lapses" -> "Moments when attention completely breaks from the task."
        "Distractibility" -> "Overall measure of how easily distracted. Some distractibility is normal."
        "Blink Rate" -> "Blinks per minute. Excessive blinking can indicate stress or hyperactivity."
        "Face Visibility" -> "Percentage of time the face was visible to the camera."
        "Facial Movement" -> "Amount of facial movement during task. Some movement is completely normal."
        "Emotion Changes" -> "Frequency of emotional expression changes. Rapid changes can indicate impulsivity."
        "Emotion Variability" -> "Intensity of emotional expression changes. Some variability is normal."
        "Fidgeting Score" -> "Small repeated movements like hand fidgeting. Some fidgeting is normal and not concerning."
        "Direction Changes" -> "How often movement direction changes. Rapid shifts can indicate restlessness."
        "Sudden Movements" -> "Quick, unexpected movements. Can indicate impulsivity if frequent."
        "Restlessness" -> "Overall physical activity level, reflecting larger body movements rather than small fidgets."

        // Domain scores
        "Inattention" -> "Measures difficulty maintaining focus and completing tasks without distraction."
        "Hyperactivity" -> "Measures excessive movement, fidgeting, and physical restlessness."
        "Impulsivity" -> "Measures acting without thinking, interrupting, and difficulty waiting."

        else -> "Metric that contributes to the overall assessment."
    }
}