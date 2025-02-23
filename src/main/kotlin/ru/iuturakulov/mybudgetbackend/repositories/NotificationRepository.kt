package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationEntity
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationTable

class NotificationRepository {

    fun createNotification(notification: NotificationEntity) = transaction {
        NotificationTable.insert { statement ->
            statement[id] = notification.id
            statement[userId] = notification.userId
            statement[type] = notification.type
            statement[message] = notification.message
            statement[projectId] = notification.projectId
            statement[createdAt] = notification.createdAt
            statement[isRead] = notification.isRead
        }
    }

    fun getUserNotifications(userId: String): List<NotificationEntity> = transaction {
        NotificationTable.selectAll().where { NotificationTable.userId eq userId }
            .map { NotificationTable.fromRow(it) }
    }

    fun markNotificationAsRead(notificationId: String): Boolean = transaction {
        NotificationTable.update({ NotificationTable.id eq notificationId }) {
            it[isRead] = true
        } > 0
    }

    fun deleteNotification(notificationId: String): Boolean = transaction {
        NotificationTable.deleteWhere { id eq notificationId } > 0
    }
}
