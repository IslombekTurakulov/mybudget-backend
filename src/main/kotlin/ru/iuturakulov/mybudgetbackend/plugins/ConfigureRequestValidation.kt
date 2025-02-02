package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginBody

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<LoginBody> { login ->
            login.validation()
            ValidationResult.Valid
        }
    }
}