package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.notification.FCMNotificationTable

class FCMNotificationTableRepository {

    fun find(userId: String, projectId: String): List<String> = transaction {
        FCMNotificationTable
            .select(FCMNotificationTable.preferences)
            .where {
                FCMNotificationTable.userId eq userId and
                        (FCMNotificationTable.projectId eq projectId)
            }.firstNotNullOfOrNull { it[FCMNotificationTable.preferences] }
            ?.split(",")
            ?.filter(String::isNotBlank)
            ?: emptyList()
    }

    fun upsert(userId: String, projectId: String, types: List<String>) = transaction {
        val csv = types.joinToString(",")
        val updated = FCMNotificationTable.update({
            (FCMNotificationTable.userId eq userId) and
                    (FCMNotificationTable.projectId eq projectId)
        }) {
            it[preferences] = csv
        }

        if (updated == 0) {
            FCMNotificationTable.insert {
                it[this.userId] = userId
                it[this.projectId] = projectId
                it[this.preferences] = csv
            }
        }
    }
}
