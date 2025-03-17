package ru.iuturakulov.mybudgetbackend.models.transaction

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType(@JsonValue val type: String) {
    @JsonEnumDefaultValue
    INCOME("income"),
    EXPENSE("expense");
}