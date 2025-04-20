package ru.iuturakulov.mybudgetbackend.models.analytics

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import kotlinx.serialization.Serializable

@Serializable
enum class AnalyticsExportFormat {
    @JsonEnumDefaultValue
    PDF,
    CSV;

    companion object {
        fun safeValueOf(v: String) =
            entries.firstOrNull { it.name.equals(v, true) }
    }
}
