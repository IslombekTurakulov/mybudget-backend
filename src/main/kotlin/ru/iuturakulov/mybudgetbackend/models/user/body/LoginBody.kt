package ru.iuturakulov.mybudgetbackend.models.user.body

import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class LoginBody(
   val email: String,
   val password: String,
) {
    fun validation() {
        validate(this) {
            validate(LoginBody::email).isNotNull().isEmail()
            validate(LoginBody::password).isNotNull().hasSize(4, 15)
        }
    }
}
