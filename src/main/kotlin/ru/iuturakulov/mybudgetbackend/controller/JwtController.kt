package ru.iuturakulov.mybudgetbackend.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody
import java.util.*

object JwtController {
    private const val SECRET = "zAP5MBA4B4Ijz0MZaS48"
    private const val ISSUER = "iuturakulov"
    private const val VALIDITY_MS = 24 * 60 * 60 * 1000L
    private val ALGORITHM = Algorithm.HMAC512(SECRET)

    val verifier: JWTVerifier by lazy {
        JWT
            .require(ALGORITHM)
            .withIssuer(ISSUER)
            .build()
    }

    fun tokenProvider(jwtTokenBody: JwtTokenBody): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(ISSUER)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withExpiresAt(getExpiration())
        .sign(ALGORITHM)

    private fun getExpiration() = Date(System.currentTimeMillis() + VALIDITY_MS)
}
