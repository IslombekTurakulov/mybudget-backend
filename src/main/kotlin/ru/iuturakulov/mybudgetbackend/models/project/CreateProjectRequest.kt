package ru.iuturakulov.mybudgetbackend.models.project

import org.valiktor.functions.hasSize
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

data class CreateProjectRequest(
    val name: String,
    val description: String,
    val budgetLimit: Double
) {
    fun validation() {
        validate(this) {
            validate(CreateProjectRequest::name).isNotBlank().hasSize(3, 64)
            if (description.isNotBlank()) {
                validate(CreateProjectRequest::description).hasSize(3, 255)
            }
            validate(CreateProjectRequest::budgetLimit).isGreaterThan(0.0)
        }
    }
}
