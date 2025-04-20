package ru.iuturakulov.mybudgetbackend.models.analytics

data class ProjectAnalyticsDto(
    val projectId: String,
    val projectName: String,
    val totalAmount: Double,
    val totalCount: Int,
    val averageAmount: Double,
    val minAmount: Double,
    val maxAmount: Double,
    val amountSpent: Double,
    val budgetLimit: Double,
    val categoryDistribution: List<OverviewCategoryStats>,
    val periodDistribution: List<PeriodStats>,
    val userDistribution:   List<UserStats>
)
