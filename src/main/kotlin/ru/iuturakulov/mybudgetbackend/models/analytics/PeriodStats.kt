package ru.iuturakulov.mybudgetbackend.models.analytics

data class PeriodStats(
    val period: String,  // Например: "Январь 2025", "Февраль 2025"
    val totalAmount: Double,
    val count: Int
)
