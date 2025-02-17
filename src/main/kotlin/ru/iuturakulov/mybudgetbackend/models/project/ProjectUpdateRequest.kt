package ru.iuturakulov.mybudgetbackend.models.project

data class ProjectUpdateRequest(
    val name: String?,
    val description: String?,
    val budgetLimit: Double?,
    val amountSpent: Double?,
    val status: String?
)