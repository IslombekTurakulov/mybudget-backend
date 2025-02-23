package ru.iuturakulov.mybudgetbackend.models.project

data class UpdateProjectRequest(
    val name: String?,
    val description: String?,
    val budgetLimit: Double?
)
