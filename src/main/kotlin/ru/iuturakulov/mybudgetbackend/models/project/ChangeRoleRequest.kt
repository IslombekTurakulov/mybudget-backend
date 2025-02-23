package ru.iuturakulov.mybudgetbackend.models.project

import ru.iuturakulov.mybudgetbackend.models.UserRole

data class ChangeRoleRequest(
    val userId: String,
    val role: UserRole
)
