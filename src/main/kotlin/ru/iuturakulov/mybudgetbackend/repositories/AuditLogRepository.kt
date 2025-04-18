package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogEntity
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogTable

class AuditLogRepository {

    /** Базовый отсортированный запрос по всем записям */
    private fun baseQuery(): Query =
        AuditLogTable
            .selectAll()
            .orderBy(AuditLogTable.timestamp, SortOrder.DESC)

    /**
     * Сохранить одну запись лога
     */
    fun saveLog(log: AuditLogEntity) = transaction {
        AuditLogTable.insert { row ->
            row[id] = log.id
            row[userId] = log.userId
            row[action] = log.action
            row[timestamp] = log.timestamp
        }
    }

    /**
     * Получить логи по одному пользователю, в порядке убывания времени
     */
    fun getLogsForUser(userId: String, limit: Int = 50): List<AuditLogEntity> = transaction {
        baseQuery()
            .andWhere { AuditLogTable.userId eq userId }
            .limit(limit)
            .map { AuditLogTable.fromRow(it) }
    }

    /**
     * Получить логи по всем пользователям, в порядке убывания времени
     */
    fun getAllLogs(limit: Int = 100): List<AuditLogEntity> = transaction {
        baseQuery()
            .limit(limit)
            .map { AuditLogTable.fromRow(it) }
    }
}