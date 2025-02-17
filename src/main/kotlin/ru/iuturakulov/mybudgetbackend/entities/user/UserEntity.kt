package ru.iuturakulov.mybudgetbackend.entities.user

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

@Serializable
data class UserEntity(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val isEmailVerified: Boolean = false,
    val emailVerificationCode: String? = null,
    val passwordResetCode: String? = null,
    val createdAt: Long
) {
    companion object {
        fun fromRow(row: ResultRow) = UserEntity(
            id = row[UserTable.id],
            name = row[UserTable.name],
            email = row[UserTable.email],
            password = row[UserTable.password],
            isEmailVerified = row[UserTable.isEmailVerified],
            emailVerificationCode = row[UserTable.emailVerificationCode],
            passwordResetCode = row[UserTable.passwordResetCode],
            createdAt = row[UserTable.createdAt]
        )
    }
}