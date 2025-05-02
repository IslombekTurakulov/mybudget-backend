package ru.iuturakulov.mybudgetbackend.services

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.invitation.InvitationEntity
import ru.iuturakulov.mybudgetbackend.entities.invitation.InvitationTable
import ru.iuturakulov.mybudgetbackend.models.UserRole
import java.io.ByteArrayOutputStream
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
        val cooldownTime = 30 * 1000L // 30 секунд (в миллисекундах)

        return if (System.currentTimeMillis() - lastSent < cooldownTime) {
            true // Приглашение уже отправлялось недавно
        } else {
            invitationCooldown[key] = System.currentTimeMillis() // Обновляем время последней отправки
            false // Можно отправлять новое приглашение
        }
    }

    fun generateInvitation(projectId: String, email: String?, role: UserRole): String = transaction {
        val code = UUID.randomUUID().toString().substring(0, 8)
        InvitationTable.insert {
            it[InvitationTable.id] = UUID.randomUUID().toString()
            it[InvitationTable.projectId] = projectId
            it[InvitationTable.email] = email
            it[InvitationTable.code] = code
            it[InvitationTable.role] = role
            it[InvitationTable.createdAt] = System.currentTimeMillis()
        }
        return@transaction code
    }

    fun deleteInvitation(inviteId: String) = transaction {
        InvitationTable.deleteWhere { id eq inviteId }
    }

    fun sendInvitationEmail(email: String, inviteCode: String, projectName: String) {
        EmailService().sendEmail(
            toEmail = email,
            subject = "Приглашение в проект \"${projectName}\"",
            message = "Вы приглашены в проект. Код приглашения: $inviteCode"
        )
    }

    fun generateQrCodeBase64(text: String, size: Int = 250): String {
        val deeplink = "mybudget://invite?code=$text"
        val bitMatrix = QRCodeWriter().encode(deeplink, BarcodeFormat.QR_CODE, size, size)
        val pngStream = ByteArrayOutputStream().also {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", it)
        }
        return Base64.getEncoder()
            .encodeToString(pngStream.toByteArray())
    }
}
