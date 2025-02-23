package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogEntity
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogTable

class AuditLogRepository {

    fun saveLog(log: AuditLogEntity) = transaction {
        AuditLogTable.insert {
            it[id] = log.id
            it[userId] = log.userId
            it[action] = log.action
            it[timestamp] = log.timestamp
        }
    }

    fun getLogsForUser(userId: String, limit: Int = 50): List<AuditLogEntity> = transaction {
        AuditLogTable.selectAll().where { AuditLogTable.userId eq userId }
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { AuditLogTable.fromRow(it) }
    }

    fun getAllLogs(limit: Int = 100): List<AuditLogEntity> = transaction {
        AuditLogTable.selectAll()
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { AuditLogTable.fromRow(it) }
    }
}
