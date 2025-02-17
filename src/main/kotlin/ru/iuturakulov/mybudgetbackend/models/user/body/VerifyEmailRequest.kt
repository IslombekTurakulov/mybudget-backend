package ru.iuturakulov.mybudgetbackend.models.user.body

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val verificationCode: String,
) {
    fun validation() {
        validate(this) {
            validate(VerifyEmailRequest::email).isNotBlank().isEmail().hasSize(5, 128)
            validate(VerifyEmailRequest::verificationCode).isNotBlank().isNotNull()
        }
    }
}
