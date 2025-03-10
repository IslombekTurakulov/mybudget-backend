package ru.iuturakulov.mybudgetbackend.entities.participants

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.models.UserRole

object ParticipantTable : Table("participants") {
    val id = varchar("id", 36) // UUID участника
    val projectId = reference("project_id", ProjectsTable.id) // Связь с проектом
    val userId = reference("user_id", UserTable.id) // Связь с пользователем
    val name = varchar("name", 64) // Храним имя участника отдельно
    val email = varchar("email", 128) // Храним email отдельно
    val role = enumerationByName("role", 10, UserRole::class)
    val createdAt = long("created_at").default(System.currentTimeMillis())

    fun fromRow(row: ResultRow) = ParticipantEntity(
        id = row[id],
        projectId = row[projectId],
        userId = row[userId],
        name = row[name],
        email = row[email],
        role = row[role],
        createdAt = row[createdAt]
    )

    override val primaryKey = PrimaryKey(id)
}
