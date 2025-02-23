package ru.iuturakulov.mybudgetbackend.extensions

import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogEntity
import ru.iuturakulov.mybudgetbackend.repositories.AuditLogRepository

class AuditLogService(private val auditLogRepository: AuditLogRepository) {

    fun logAction(userId: String, action: String) {
        transaction {
            auditLogRepository.saveLog(
                log = AuditLogEntity(
                    userId = userId,
                    action = action,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun getUserLogs(userId: String, limit: Int = 50): List<AuditLogEntity> {
        return auditLogRepository.getLogsForUser(userId, limit)
    }

    fun getAllLogs(limit: Int = 100): List<AuditLogEntity> {
        return auditLogRepository.getAllLogs(limit)
    }
}
