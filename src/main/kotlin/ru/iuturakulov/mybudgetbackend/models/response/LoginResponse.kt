package ru.iuturakulov.mybudgetbackend.models.response

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val token: String
)