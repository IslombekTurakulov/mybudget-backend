package ru.iuturakulov.mybudgetbackend.models.analytics

data class AnalyticsFilter(
    val fromDate: Long? = null,  // Начало периода (timestamp)
    val toDate: Long? = null,    // Конец периода (timestamp)
    val categories: List<String>? = null // Список категорий для анализа
)
