package ru.iuturakulov.mybudgetbackend.models.analytics

//data class OverviewAnalyticsDto(
//    val totalSpent: Double,                        // Общая сумма расходов по всем проектам
//    val categoryDistribution: List<CategoryStats>, // Распределение расходов по категориям
//    val periodDistribution: List<PeriodStats>,     // Распределение расходов по периодам
//    val projectComparison: List<ProjectComparison> // Сравнение проектов по затратам
//)

data class OverviewAnalyticsDto(
    val totalAmount: Double,                                 // общая сумма по всем проектам
    val categoryDistribution: List<OverviewCategoryStats>,
    val periodDistribution: List<OverviewPeriodStats>,
    val projectComparison: List<ProjectComparisonStats>
)

data class OverviewCategoryStats(
    val category: String,
    val amount: Double,
    val percentage: Double                                 // [0..100], для подписей на клиенте
)

data class OverviewPeriodStats(
    val period: String,    // например "2025-04"
    val amount: Double
)

data class ProjectComparisonStats(
    val projectId: String,
    val projectName: String,
    val amount: Double
)