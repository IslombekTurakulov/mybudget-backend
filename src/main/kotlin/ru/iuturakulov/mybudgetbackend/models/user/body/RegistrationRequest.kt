package ru.iuturakulov.mybudgetbackend.models.user.body

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class RegistrationRequest(
    val name: String,
    val email: String,
    val password: String,
    val code: String
) {
    fun validation() {
        validate(this) {
            validate(RegistrationRequest::name).isNotBlank().isNotNull().hasSize(4, 64)
            validate(RegistrationRequest::email).isNotBlank().isEmail().hasSize(5, 128)
            validate(RegistrationRequest::password).isNotBlank().hasSize(8, 32)
        }
    }
}