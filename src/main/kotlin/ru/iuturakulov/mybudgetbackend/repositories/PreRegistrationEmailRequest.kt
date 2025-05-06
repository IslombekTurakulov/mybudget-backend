package ru.iuturakulov.mybudgetbackend.repositories

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotBlank
import org.valiktor.validate

@Serializable
data class PreRegistrationEmailRequest(
    val email: String
)

fun PreRegistrationEmailRequest.validate() {
    validate(this) {
        validate(PreRegistrationEmailRequest::email)
            .isNotBlank()
            .isEmail()
    }
}