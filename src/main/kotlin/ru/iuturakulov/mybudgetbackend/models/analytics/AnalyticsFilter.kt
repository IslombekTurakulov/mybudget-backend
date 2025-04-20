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
) {
    fun toSuffix(): String {
        val period = listOfNotNull(fromDate, toDate)
            .joinToString("_") { it.toString() }
            .ifBlank { "all_time" }
        return "${period}_${granularity.name.lowercase()}"
    }
}

@Serializable
enum class Granularity {
    DAY,
    WEEK,

    @JsonEnumDefaultValue
    MONTH,
    YEAR;

    companion object {
        fun safeValueOf(v: String): Granularity? {
            return entries.firstOrNull {
                it.name.equals(v, true)
            }
        }
    }
}
