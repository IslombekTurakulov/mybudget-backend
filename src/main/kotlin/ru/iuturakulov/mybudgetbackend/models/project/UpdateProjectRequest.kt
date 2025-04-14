package ru.iuturakulov.mybudgetbackend.models.project

import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectStatus

data class UpdateProjectRequest(
    val name: String?,
    val description: String?,
    val budgetLimit: Double?,
    val status: ProjectStatus? = null,
)
