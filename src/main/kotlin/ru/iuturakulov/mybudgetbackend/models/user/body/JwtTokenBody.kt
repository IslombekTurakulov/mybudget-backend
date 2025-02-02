package ru.iuturakulov.mybudgetbackend.models.user.body

import io.ktor.server.auth.*

data class JwtTokenBody(val userId: String, val email: String) : Principal