package ru.iuturakulov.mybudgetbackend.extensions

import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.controller.user.DateTimeProvider
import ru.iuturakulov.mybudgetbackend.controller.user.SystemDateTimeProvider
import ru.iuturakulov.mybudgetbackend.entities.audit.AuditLogEntity
import ru.iuturakulov.mybudgetbackend.repositories.AuditLogRepository
import java.util.UUID

class AuditLogService(
    private val repository: AuditLogRepository,
    private val clock: DateTimeProvider = SystemDateTimeProvider()
) {

    /**
     * Логируем действие — формируем сущность и передаём в репозиторий.
     */
    fun logAction(userId: String, action: String) {
        val log = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            action = action,
            timestamp = clock.now().toEpochMilli()
        )
        repository.saveLog(log)
    }

    /**
     * Получить последние логи конкретного пользователя
     */
    fun getUserLogs(userId: String, limit: Int = 50): List<AuditLogEntity> =
        repository.getLogsForUser(userId, limit)

    /**
     * Получить последние логи по всем пользователям
     */
    fun getAllLogs(limit: Int = 100): List<AuditLogEntity> =
        repository.getAllLogs(limit)
}