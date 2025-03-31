package ru.iuturakulov.mybudgetbackend.models.settings

import kotlinx.serialization.Serializable
import org.valiktor.functions.hasSize
import org.valiktor.functions.isEmail
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotBlank
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class UserSettingsRequest(
    val name: String,
    val email: String,
    val language: String,
    val notificationsEnabled: Boolean
) {
    fun validation() {
        validate(this) {
            validate(UserSettingsRequest::name).isNotBlank().isNotNull().hasSize(min = 4, max = 64)
            validate(UserSettingsRequest::email).isNotBlank().isEmail().hasSize(5, 128)
            validate(UserSettingsRequest::language).isIn("Русский", "English")
        }
    }
}
