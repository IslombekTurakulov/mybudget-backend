package ru.iuturakulov.mybudgetbackend.entities.notification

data class NotificationEntity(
    val id: String,
    val userId: String,  // Кому предназначено уведомление
    val type: NotificationType, // Тип уведомления
    val message: String,
    val projectId: String?,
    val createdAt: Long,
    val isRead: Boolean = false
)

enum class NotificationType {
    PROJECT_INVITE,
    ROLE_CHANGE,
    TRANSACTION_ADDED,
    TRANSACTION_REMOVED,
    PROJECT_EDITED,
    PROJECT_REMOVED,
    SYSTEM_ALERT
}
