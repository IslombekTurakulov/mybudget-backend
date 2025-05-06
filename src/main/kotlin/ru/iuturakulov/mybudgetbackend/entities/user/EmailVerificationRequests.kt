package ru.iuturakulov.mybudgetbackend.entities.user

import org.jetbrains.exposed.sql.Table

object EmailVerificationTable : Table("email_verifications") {
    val email = varchar("email", 128).uniqueIndex()
    val code = varchar("code", 6)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(email)
}
