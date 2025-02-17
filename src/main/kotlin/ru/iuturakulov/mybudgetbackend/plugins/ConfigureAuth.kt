package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import ru.iuturakulov.mybudgetbackend.config.JwtConfig
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Application.configureAuth() {
    install(Authentication) {
        jwt {
            provideJwtAuthConfig(this)
        }
    }
}

fun provideJwtAuthConfig(jwtConfig: JWTAuthenticationProvider.Config) {
    jwtConfig.verifier(JwtConfig.verifier)
    jwtConfig.realm = "iuturakulov"
    jwtConfig.validate {
        val userId = it.payload.getClaim("userId").asString()
        JwtTokenBody(userId)
    }
}