package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.fcm.DeviceTokens
import ru.iuturakulov.mybudgetbackend.entities.notification.FCMNotificationTable
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.models.fcm.DeviceTokenWithLanguageCode

class DeviceTokenRepository {

    /**
     * Регистрирует или обновляет FCM-токен для пользователя.
     */
    fun registerOrUpdate(userId: String, token: String, platform: String, language: String) = transaction {
        val updated = DeviceTokens.update({ DeviceTokens.token eq token }) {
            it[DeviceTokens.userId] = userId
            it[DeviceTokens.platform] = platform
            it[DeviceTokens.language] = language
        }
        if (updated == 0) {
            DeviceTokens.insert {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.token] = token
                it[DeviceTokens.platform] = platform
                it[DeviceTokens.language] = language
            }
        }
    }

    /**
     * Возвращает список FCM-токенов для переданных userId и список локализаций у userIds.
     */
    fun findTokensAndLanguageCodeByUserIds(
        userIds: List<String>
    ): List<DeviceTokenWithLanguageCode> = transaction {
        (DeviceTokens innerJoin UserTable)
            .selectAll().where { DeviceTokens.userId inList userIds }
            .map { row ->
                DeviceTokenWithLanguageCode(
                    token = row[DeviceTokens.token],
                    languageCode = row[DeviceTokens.language],
                    userId = row[UserTable.id]
                )
            }
    }

    /**
     * Возвращает список userId-участников проекта.
     */
    fun findParticipantIds(projectId: String): List<String> = transaction {
        ParticipantTable
            .select(ParticipantTable.userId)
            .where { ParticipantTable.projectId eq projectId }
            .map { it[ParticipantTable.userId] }
    }

    /**
     * Проверяет, включены ли глобально уведомления для данного пользователя.
     */
    fun isNotificationsEnabled(userId: String): Boolean = transaction {
        UserTable
            .select(UserTable.notificationsEnabled)
            .where { UserTable.id eq userId }
            .map { it[UserTable.notificationsEnabled] }
            .firstOrNull()
            ?: false
    }

    /**
     * Проверяет, включён ли данный тип уведомления в настройках пользователя
     * для конкретного проекта.
     */
    fun hasProjectPref(userId: String, projectId: String, type: NotificationType): Boolean = transaction {
        FCMNotificationTable
            .select(FCMNotificationTable.preferences)
            .where {
                (FCMNotificationTable.userId eq userId) and
                        (FCMNotificationTable.projectId eq projectId)
            }.firstNotNullOfOrNull { it[FCMNotificationTable.preferences] }
            ?.split(",")
            ?.any { it == type.name }
            ?: false
    }
}