package ru.iuturakulov.mybudgetbackend.models.user.body

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ForgetPasswordEmailRequest(val email: String) {
    fun validation() {
        validate(this) {
            validate(ForgetPasswordEmailRequest::email).isNotNull().isEmail()
        }
    }
}