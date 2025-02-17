package ru.iuturakulov.mybudgetbackend.models.user.body

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
) {
    fun validation() {
        validate(this) {
            validate(RefreshTokenRequest::refreshToken).isNotBlank().isNotNull()
        }
    }
}