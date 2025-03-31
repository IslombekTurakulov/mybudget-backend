package ru.iuturakulov.mybudgetbackend.services

import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationEntity
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.repositories.NotificationRepository
import java.util.*

class NotificationService(private val notificationRepository: NotificationRepository) {

    fun sendNotification(userId: String, type: NotificationType, message: String, projectId: String? = null) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            message = message,
            projectId = projectId,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
        notificationRepository.createNotification(notification)
    }

    fun getNotificationsForUser(userId: String): List<NotificationEntity> {
        return notificationRepository.getUserNotifications(userId)
    }

    fun markAsRead(notificationId: String): Boolean {
        return notificationRepository.markNotificationAsRead(notificationId)
    }

    fun deleteNotification(notificationId: String): Boolean {
        return notificationRepository.deleteNotification(notificationId)
    }
}
