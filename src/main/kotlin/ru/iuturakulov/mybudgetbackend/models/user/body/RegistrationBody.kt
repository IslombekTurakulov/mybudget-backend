package ru.iuturakulov.mybudgetbackend.models.user.body

import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class RegistrationBody(val email: String, val password: String) {
    fun validation() {
        validate(this) {
            validate(RegistrationBody::email).isNotNull().isEmail()
            validate(RegistrationBody::password).isNotNull().hasSize(4, 15)
        }
    }
}