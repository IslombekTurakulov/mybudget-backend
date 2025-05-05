package ru.iuturakulov.mybudgetbackend.controller.notification

import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationEntity
import ru.iuturakulov.mybudgetbackend.services.OverallNotificationService

class NotificationController(private val overallNotificationService: OverallNotificationService) {

    fun getUserNotifications(userId: String): List<NotificationEntity> {
        return overallNotificationService.getNotificationsForUser(userId)
    }

    fun markNotificationAsRead(userId: String, notificationId: String): Boolean {
        return overallNotificationService.markAsRead(notificationId)
    }

    fun deleteNotification(userId: String, notificationId: String): Boolean {
        return overallNotificationService.deleteNotification(notificationId)
    }
}
