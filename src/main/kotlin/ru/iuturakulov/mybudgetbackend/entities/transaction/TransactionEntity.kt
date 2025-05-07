package ru.iuturakulov.mybudgetbackend.entities.transaction

import ru.iuturakulov.mybudgetbackend.models.transaction.TransactionType

data class TransactionEntity(
    val id: String,
    val projectId: String,
    val projectName: String?,
    val userId: String,
    val userName: String,
    val name: String,
    val amount: Double,
    val category: String?,
    val categoryIcon: String?,
    val date: Long,
    val type: TransactionType,
    val images: List<String>?,
)
