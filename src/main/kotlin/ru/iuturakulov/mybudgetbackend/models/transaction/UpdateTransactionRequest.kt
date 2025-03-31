package ru.iuturakulov.mybudgetbackend.models.transaction

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateTransactionRequest(
    val transactionId: String,
    val name: String? = null,
    val amount: Double? = null,
    val category: String? = null,
    val categoryIcon: String? = null,
    val date: Long? = null,
    val type: TransactionType? = null,
    val images: List<String> = emptyList(),
) {
    fun validation() {
        validate(this) {
            validate(UpdateTransactionRequest::transactionId).isNotBlank()
            validate(UpdateTransactionRequest::amount).isNotNull().isGreaterThan(0.0)
        }
    }
}