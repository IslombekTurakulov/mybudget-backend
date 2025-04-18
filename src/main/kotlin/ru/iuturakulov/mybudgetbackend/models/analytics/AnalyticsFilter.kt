package ru.iuturakulov.mybudgetbackend.models.analytics

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import kotlinx.serialization.Serializable


/**
 * Позволяет задать:
 *  - границы периода (from, to)
 *  - «шаг» группировки: день, неделя, месяц, год
 */
data class AnalyticsFilter(
    val fromDate: Long? = null,  // Начало периода (timestamp)
    val toDate: Long? = null,    // Конец периода (timestamp)
    val categories: List<String>? = null, // Список категорий для анализа
    val granularity: Granularity = Granularity.MONTH
)

@Serializable
enum class Granularity {
    DAY,
    WEEK,

    @JsonEnumDefaultValue
    MONTH,
    YEAR
}
