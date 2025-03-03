package ru.iuturakulov.mybudgetbackend.models.analytics

data class OverviewAnalyticsDto(
    val totalSpent: Double,                        // Общая сумма расходов по всем проектам
    val categoryDistribution: List<CategoryStats>, // Распределение расходов по категориям
    val periodDistribution: List<PeriodStats>,     // Распределение расходов по периодам
    val projectComparison: List<ProjectComparison> // Сравнение проектов по затратам
)
