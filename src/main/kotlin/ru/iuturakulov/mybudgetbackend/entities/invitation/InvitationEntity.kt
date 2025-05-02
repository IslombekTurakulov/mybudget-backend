package ru.iuturakulov.mybudgetbackend.entities.invitation

import org.jetbrains.exposed.sql.ResultRow
import ru.iuturakulov.mybudgetbackend.models.UserRole

data class InvitationEntity(
    val id: String,
    val projectId: String,
    val email: String?,
    val code: String,
    val role: UserRole,
    val createdAt: Long
) {

    /**
     * Проверяет, истекло ли приглашение (если прошло больше 24 часов)
     */
    fun isExpired(expirationTimeMillis: Long = 24 * 60 * 60 * 1000): Boolean {
        return System.currentTimeMillis() - createdAt > expirationTimeMillis
    }

    companion object {

        fun fromRow(row: ResultRow) = InvitationEntity(
            id = row[InvitationTable.id],
            projectId = row[InvitationTable.projectId],
            email = row[InvitationTable.email],
            code = row[InvitationTable.code],
            role = UserRole.valueOf(row[InvitationTable.role].toString()),
            createdAt = row[InvitationTable.createdAt]
        )
    }
}
