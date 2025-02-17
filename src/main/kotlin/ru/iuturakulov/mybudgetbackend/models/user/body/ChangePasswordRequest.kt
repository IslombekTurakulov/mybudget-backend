package ru.iuturakulov.mybudgetbackend.models.user.body

import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
) {
    fun validation() {
        validate(this) {
            validate(ChangePasswordRequest::oldPassword).isNotNull().hasSize(8, 32)
            validate(ChangePasswordRequest::newPassword).isNotNull().hasSize(8, 32)
        }
    }
}