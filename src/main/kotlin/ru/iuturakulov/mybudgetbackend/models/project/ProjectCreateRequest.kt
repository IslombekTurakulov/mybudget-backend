package ru.iuturakulov.mybudgetbackend.models.project

data class ProjectCreateRequest(
    val name: String,
    val description: String?,
    val budgetLimit: Double
)