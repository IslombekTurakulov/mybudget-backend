package ru.iuturakulov.mybudgetbackend.services

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.invitation.InvitationEntity
import ru.iuturakulov.mybudgetbackend.entities.invitation.InvitationTable
import ru.iuturakulov.mybudgetbackend.models.UserRole
import java.util.*

class InvitationService {

    private val invitationCooldown = mutableMapOf<String, Long>()

    fun getInvitation(inviteCode: String) = transaction {
        InvitationTable.selectAll().where {
            (InvitationTable.code eq inviteCode)
        }.mapNotNull { InvitationEntity.fromRow(it) }.singleOrNull()
    }

    fun hasRecentInvitation(email: String, projectId: String): Boolean {
        val key = "$email-$projectId"
        val lastSent = invitationCooldown[key] ?: 0L
        val cooldownTime = 5 * 60 * 1000L // 5 минут

        return if (System.currentTimeMillis() - lastSent < cooldownTime) {
            true
        } else {
            invitationCooldown[key] = System.currentTimeMillis()
            false
        }
    }

    fun generateInvitation(projectId: String, email: String, role: UserRole): String = transaction {
        val code = UUID.randomUUID().toString().substring(0, 8)
        InvitationTable.insert {
            it[InvitationTable.projectId] = projectId
            it[InvitationTable.email] = email
            it[InvitationTable.code] = code
            it[InvitationTable.role] = role
        }
        return@transaction code
    }

    fun deleteInvitation(inviteId: String) = transaction {
        InvitationTable.deleteWhere { id eq inviteId }
    }

    fun sendInvitationEmail(email: String, inviteCode: String) {
        EmailService().sendEmail(
            toEmail = email,
            subject = "Приглашение в проект",
            message = "Вы приглашены в проект. Код приглашения: $inviteCode"
        )
    }
}
