package ru.iuturakulov.mybudgetbackend.entities.participants

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.models.UserRole

object ParticipantTable : Table("participant") {
    val id = varchar("id", 36) // UUID участника
    val projectId = reference("project_id", ProjectsTable.id) // Связь с проектом
    val userId = reference("user_id", UserTable.id) // Связь с пользователем
    val role = enumerationByName("role", 10, UserRole::class)
    val createdAt = long("created_at").default(System.currentTimeMillis())

    fun fromRow(row: ResultRow) = ParticipantEntity(
        id = row[id],
        projectId = row[projectId],
        userId = row[userId],
        name = row[UserTable.name], // Достаём имя из UserTable
        email = row[UserTable.email], // Достаём email из UserTable
        role = row[role],
        createdAt = row[createdAt]
    )

    override val primaryKey = PrimaryKey(id)
}
