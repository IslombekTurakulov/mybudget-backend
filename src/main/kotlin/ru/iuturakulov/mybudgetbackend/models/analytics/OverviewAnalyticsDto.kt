package ru.iuturakulov.mybudgetbackend.models.analytics

data class OverviewAnalyticsDto(
    val totalAmount: Double,                                 // общая сумма по всем проектам
    val categoryDistribution: List<OverviewCategoryStats>,
    val periodDistribution: List<OverviewPeriodStats>,
    val projectComparison: List<ProjectComparisonStats>,
    val totalCount: Int,
    val averageAmount: Double,
    val minAmount: Double,
    val maxAmount: Double,
)

data class OverviewCategoryStats(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int,
    val transactionInfo: List<TransactionInfo>
)

data class TransactionInfo(
    val id: String,
    val projectId: String,
    val projectName: String?,
    val name: String,
    val amount: Double,
    val date: String,
    val userName: String,
    val type: String,
    val categoryIcon: String?
)

data class OverviewPeriodStats(
    val period: String,    // например "2025-04"
    val amount: Double,
    val count: Int
)

data class ProjectComparisonStats(
    val projectId: String,
    val projectName: String,
    val amount: Double,
    val count: Int
)