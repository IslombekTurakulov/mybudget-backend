package ru.iuturakulov.mybudgetbackend.models.analytics

data class ProjectAnalyticsDto(
    val projectId: String,
    val projectName: String,
    val totalAmount: Double,
    val categoryDistribution: List<CategoryStats>,
    val periodDistribution: List<PeriodStats>,
//    val taskComparison: List<TaskStats>
)