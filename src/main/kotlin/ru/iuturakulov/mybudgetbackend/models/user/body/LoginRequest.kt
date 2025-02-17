package ru.iuturakulov.mybudgetbackend.models.user.body

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
) {
    fun validation() {
        validate(this) {
            validate(LoginRequest::email).isNotBlank().isNotNull().isEmail()
            validate(LoginRequest::password).isNotBlank().isNotNull().hasSize(6, 64)
        }
    }
}
