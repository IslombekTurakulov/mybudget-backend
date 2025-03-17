package ru.iuturakulov.mybudgetbackend.models.transaction

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

data class AddTransactionRequest(
    val name: String,
    val projectId: String,
    val amount: Double,
    val category: String? = null,
    val categoryIcon: String? = null,
    val date: Long,
    val transactionType: TransactionType? = null,
    val images: List<String>? = null,
) {

    fun validation() {
        validate(this) {
            validate(AddTransactionRequest::name).isNotBlank()
            validate(AddTransactionRequest::projectId).isNotBlank()
            validate(AddTransactionRequest::amount).isGreaterThan(0.0)
        }
    }
}
