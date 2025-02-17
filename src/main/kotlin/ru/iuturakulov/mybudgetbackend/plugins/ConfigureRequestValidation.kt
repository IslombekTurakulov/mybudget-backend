package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginRequest

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<LoginRequest> { login ->
            login.validation()
            ValidationResult.Valid
        }
    }
}