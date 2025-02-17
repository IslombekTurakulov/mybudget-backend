package ru.iuturakulov.mybudgetbackend.entities.user

import org.jetbrains.exposed.sql.Table
import java.util.*

object UserTable : Table("users") {
    val id = varchar("id", 36).default(UUID.randomUUID().toString())
    val name = varchar("name", 64)
    val email = varchar("email", 128).uniqueIndex()
    val password = varchar("password", 256)
    val isEmailVerified = bool("is_email_verified").default(false)
    val emailVerificationCode = varchar("email_verification_code", 6).nullable()
    val passwordResetCode = varchar("password_reset_code", 6).nullable()
    val createdAt = long("created_at").default(System.currentTimeMillis())

    override val primaryKey = PrimaryKey(id)
}