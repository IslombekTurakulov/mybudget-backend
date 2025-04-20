package ru.iuturakulov.mybudgetbackend.models.analytics

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import kotlinx.serialization.Serializable

@Serializable
enum class AnalyticsExportFrom {
    @JsonEnumDefaultValue
    OVERVIEW,
    PROJECT;

    companion object {
        fun safeValueOf(v: String): AnalyticsExportFrom? {
            return entries.firstOrNull { it.name.equals(v, true) }
        }
    }
}