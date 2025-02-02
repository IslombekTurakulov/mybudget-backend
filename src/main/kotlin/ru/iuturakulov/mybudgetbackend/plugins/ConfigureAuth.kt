package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import ru.iuturakulov.mybudgetbackend.controller.JwtController
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Application.configureAuth() {
    install(Authentication) {
        jwt {
            provideJwtAuthConfig(this)
        }
    }
}

fun provideJwtAuthConfig(jwtConfig: JWTAuthenticationProvider.Config) {
    jwtConfig.verifier(JwtController.verifier)
    jwtConfig.realm = "iuturakulov"
    jwtConfig.validate {
        val userId = it.payload.getClaim("userId").asString()
        val email = it.payload.getClaim("email").asString()
        JwtTokenBody(userId, email)
    }
}