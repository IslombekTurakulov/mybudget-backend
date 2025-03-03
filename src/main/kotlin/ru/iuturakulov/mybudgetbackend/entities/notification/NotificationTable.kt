package ru.iuturakulov.mybudgetbackend.entities.notification

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable

object NotificationTable : Table("notifications") {
    val id = varchar("id", 36)
    val userId = reference("user_id", UserTable.id)
    val type = enumerationByName("type", 32, NotificationType::class)
    val message = text("message")
    val projectId = reference("project_id", ProjectsTable.id).nullable()
    val createdAt = long("created_at").default(System.currentTimeMillis())
    val isRead = bool("is_read").default(false)

    override val primaryKey = PrimaryKey(id)

    fun fromRow(row: ResultRow) = NotificationEntity(
        id = row[id],
        userId = row[userId],
        type = row[type],
        message = row[message],
        projectId = row[projectId],
        createdAt = row[createdAt],
        isRead = row[isRead]
    )
}
