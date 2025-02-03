package ru.iuturakulov.mybudgetbackend.models.user.body

import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class RegistrationBody(val name: String, val email: String, val password: String) {
    fun validation() {
        validate(this) {
            validate(RegistrationBody::name).isNotNull().hasSize(1, 64)
            validate(RegistrationBody::email).isNotNull().isEmail().hasSize(3, 64)
            validate(RegistrationBody::password).isNotNull().hasSize(4, 16)
        }
    }
}