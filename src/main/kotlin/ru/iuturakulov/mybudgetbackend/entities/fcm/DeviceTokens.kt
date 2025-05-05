package ru.iuturakulov.mybudgetbackend.entities.fcm

import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable

object DeviceTokens : Table("device_tokens") {
    val userId = varchar("user_id", 36).references(UserTable.id)
    val token = varchar("token", 256).uniqueIndex()
    val platform = varchar("platform", 16)
    val language = varchar("language", 16).default("ru")
}
