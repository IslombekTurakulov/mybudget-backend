package ru.iuturakulov.mybudgetbackend.models.user.body

data class UserProfileBody(
    val firstName: String?,
    val lastName: String?,
    val userDescription: String?,
)