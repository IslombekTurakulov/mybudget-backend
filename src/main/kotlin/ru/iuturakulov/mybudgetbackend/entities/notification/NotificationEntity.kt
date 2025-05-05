package ru.iuturakulov.mybudgetbackend.entities.notification

import kotlinx.serialization.Serializable

data class NotificationEntity(
    val id: String,
    val userId: String,  // Кому предназначено уведомление
    val type: NotificationType, // Тип уведомления
    val message: String,
    val projectId: String?,
    val createdAt: Long,
    val isRead: Boolean = false
)

@Serializable
enum class NotificationType {
    PROJECT_INVITE_SEND,
    PROJECT_INVITE_ACCEPT,
    PARTICIPANT_ROLE_CHANGE,
    PARTICIPANT_REMOVED,
    TRANSACTION_ADDED,
    TRANSACTION_UPDATED,
    TRANSACTION_REMOVED,
    PROJECT_EDITED,
    PROJECT_REMOVED,
    PROJECT_ARCHIVED,
    PROJECT_UNARCHIVED,
    BUDGET_THRESHOLD,
    SYSTEM_ALERT
}

@Serializable
data class NotificationPayload(
    val type: NotificationType,
    val projectId: String? = null,
    val transactionId: String? = null,
    val senderId: String? = null,
    val customData: Map<String, String>? = null
)

