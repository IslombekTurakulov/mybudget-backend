package ru.iuturakulov.mybudgetbackend.entities.invitation

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.models.UserRole
import java.util.*

object InvitationTable : Table("invitations") {
    val id = varchar("id", 36)
    val projectId = reference("project_id", ProjectsTable.id, onDelete = ReferenceOption.CASCADE)
    val email = varchar("email", 128)
    val code = varchar("code", 8).uniqueIndex()
    val role = enumerationByName("role", 10, UserRole::class)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

