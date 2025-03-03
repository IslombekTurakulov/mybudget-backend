package ru.iuturakulov.mybudgetbackend.models.analytics

data class ProjectComparison(
    val projectId: String,
    val projectName: String,
    val totalSpent: Double
)