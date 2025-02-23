package ru.iuturakulov.mybudgetbackend.models.transaction

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

data class AddTransactionRequest(
    val name: String,
    val projectId: String,
    val amount: Double,
    val category: String,
    val categoryIcon: String,
    val date: Long
) {
    fun validation() {
        validate(this) {
            validate(AddTransactionRequest::name).isNotBlank()
            validate(AddTransactionRequest::projectId).isNotBlank()
            validate(AddTransactionRequest::amount).isGreaterThan(0.0)
        }
    }
}
