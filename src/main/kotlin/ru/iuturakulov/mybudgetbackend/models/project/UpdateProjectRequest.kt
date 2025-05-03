package ru.iuturakulov.mybudgetbackend.models.project

import org.valiktor.functions.hasSize
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotBlank
import org.valiktor.validate
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectStatus

data class UpdateProjectRequest(
    val name: String?,
    val description: String?,
    val budgetLimit: Double?,
    val category: String?,
    val categoryIcon: String,
    val status: ProjectStatus? = null,
) {
    fun validation() {
        validate(this) {
            validate(UpdateProjectRequest::name).isNotBlank().hasSize(1, 64)
            if (description?.isNotBlank() == true) {
                validate(UpdateProjectRequest::description).hasSize(1, 1000)
            }
            validate(UpdateProjectRequest::budgetLimit).isGreaterThan(0.0)
        }
    }
}

