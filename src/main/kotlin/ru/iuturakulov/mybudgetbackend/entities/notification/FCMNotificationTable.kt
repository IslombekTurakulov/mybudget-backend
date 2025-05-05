package ru.iuturakulov.mybudgetbackend.entities.notification

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable

object FCMNotificationTable : Table("project_notification_prefs") {
    val projectId = reference("project_id", ProjectsTable.id)
    val userId = reference("user_id", UserTable.id)
    val preferences = text("preferences") // CSV, например "TRANSACTION_ADDED,PROJECT_EDITED"
    override val primaryKey = PrimaryKey(projectId, userId)
}
