package ru.iuturakulov.mybudgetbackend.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.repositories.DeviceTokenRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository

class NotificationGuard(
    private val deviceRepo: DeviceTokenRepository,
    private val projectRepo: ProjectRepository
) {

    fun canReceiveNotification(
        userId: String,
        projectId: String?,
        type: NotificationType
    ): Boolean = transaction {
        // Отключены глобально
        if (!deviceRepo.isNotificationsEnabled(userId)) return@transaction false

        // Тип уведомления не разрешён по роли
        val role = projectId?.let { projectRepo.getUserRoleInProject(userId, it) }
            ?: UserRole.VIEWER

        if (!NotificationPermissions.isAllowed(role, type)) return@transaction false

        // Пользователь вручную отключил этот тип уведомлений для проекта
        if (projectId != null && !deviceRepo.hasProjectPref(userId, projectId, type)) return@transaction false

        // Всё ок
        return@transaction true
    }

    fun filterReceivers(
        candidateUserIds: List<String>,
        type: NotificationType,
        projectId: String?
    ): List<String> {
        val filter = candidateUserIds.filter {
            canReceiveNotification(it, projectId, type)
        }
        return filter
    }
}
