package ru.iuturakulov.mybudgetbackend.entities.transaction

import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType

data class TransactionEntity(
    val id: String,
    val projectId: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val category: String?,
    val categoryIcon: String?,
    val date: Long,
    val transactionType: TransactionType,
    val images: List<String>?,
)

