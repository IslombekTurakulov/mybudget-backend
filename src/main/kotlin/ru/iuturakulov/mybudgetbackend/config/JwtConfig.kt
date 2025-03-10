package ru.iuturakulov.mybudgetbackend.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*

object JwtConfig {
    // https://www.grc.com/passwords.htm
    private const val SECRET = "Y9OoBfeZzuNMURbFomX9K7h1w7RNQBsasHPxG7c4WPaV4KuJ86dnozQc2x208G7" // to env
    private const val ISSUER = "iuturakulov"
    private const val AUDIENCE = "mybudget-users"
    private const val VALIDITY_MS = 24 * 60 * 60 * 1000L
    private val ALGORITHM = Algorithm.HMAC512(SECRET)

    val verifier: JWTVerifier by lazy {
        JWT
            .require(ALGORITHM)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .build()
    }

    fun generateToken(userId: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("userId", userId)
            .withExpiresAt(getExpiration())
            .sign(ALGORITHM)
    }

    /**
     * Проверка токена (используется в `JWTConfig`)
     */
    fun verify(token: String): DecodedJWT? {
        return try {
            JWT.require(ALGORITHM)
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }

    private fun getExpiration() = Date(System.currentTimeMillis() + VALIDITY_MS)
}
