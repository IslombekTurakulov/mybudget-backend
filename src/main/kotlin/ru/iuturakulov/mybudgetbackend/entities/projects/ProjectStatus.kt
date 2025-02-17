package ru.iuturakulov.mybudgetbackend.entities.projects

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable

@Serializable
enum class ProjectStatus(@JsonValue val type: String) {
    @JsonEnumDefaultValue
    UNKNOWN("unknown"),
    ACTIVE("active"),
    ARCHIVED("archived"),
    DELETED("deleted")
}
