package ru.iuturakulov.mybudgetbackend.controller.notification

import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationEntity
import services.NotificationService

class NotificationController(private val notificationService: NotificationService) {

    fun getUserNotifications(userId: String): List<NotificationEntity> {
        return notificationService.getNotificationsForUser(userId)
    }

    fun markNotificationAsRead(userId: String, notificationId: String): Boolean {
        return notificationService.markAsRead(notificationId)
    }

    fun deleteNotification(userId: String, notificationId: String): Boolean {
        return notificationService.deleteNotification(notificationId)
    }
}
