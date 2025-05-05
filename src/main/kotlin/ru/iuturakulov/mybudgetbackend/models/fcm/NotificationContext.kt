package ru.iuturakulov.mybudgetbackend.models.fcm

data class NotificationContext(
    val actorId: String?,
    val actor: String? = null,
    val projectId: String? = null,
    val projectName: String? = null,
    val transactionId: String? = null,
    val transactionName: String? = null,
    val beforeSpent: Double? = null,
    val afterSpent: Double? = null,
    val budgetLimit: Double? = null,
    val details: String? = null,
    val systemMessage: String? = null,
)
