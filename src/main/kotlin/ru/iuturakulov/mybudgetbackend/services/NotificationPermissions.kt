package ru.iuturakulov.mybudgetbackend.services

import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.models.UserRole

object NotificationPermissions {

    private val permissionsByRole: Map<UserRole, Set<NotificationType>> = mapOf(
        UserRole.OWNER to NotificationType.entries.toSet(), // всё доступно

        UserRole.EDITOR to setOf(
            NotificationType.TRANSACTION_ADDED,
            NotificationType.TRANSACTION_UPDATED,
            NotificationType.TRANSACTION_REMOVED,
            NotificationType.BUDGET_THRESHOLD,
            NotificationType.PROJECT_EDITED,
            NotificationType.PROJECT_REMOVED,
            NotificationType.PROJECT_ARCHIVED,
            NotificationType.PROJECT_UNARCHIVED,
            NotificationType.PARTICIPANT_ROLE_CHANGE,
        ),

        UserRole.VIEWER to setOf(
            NotificationType.TRANSACTION_ADDED,
            NotificationType.BUDGET_THRESHOLD,
            NotificationType.PROJECT_EDITED,
            NotificationType.PROJECT_REMOVED,
            NotificationType.PROJECT_ARCHIVED,
            NotificationType.PROJECT_UNARCHIVED,
            NotificationType.SYSTEM_ALERT,
        )
    )

    fun isAllowed(role: UserRole, type: NotificationType): Boolean {
        return permissionsByRole[role]?.contains(type) == true
    }

    fun allowedTypesFor(role: UserRole): Set<NotificationType> {
        return permissionsByRole[role].orEmpty()
    }
}
