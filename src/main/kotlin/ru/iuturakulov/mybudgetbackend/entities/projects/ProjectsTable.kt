package ru.iuturakulov.mybudgetbackend.entities.projects

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable

object ProjectsTable : Table("projects") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val budgetLimit = decimal("budget_limit", 12, 2)
    val amountSpent = decimal("amount_spent", 12, 2).default(0.0.toBigDecimal())
    val status = enumerationByName("status", 10, ProjectStatus::class)
    val createdAt = long("created_at").default(System.currentTimeMillis())
    val lastModified = long("last_modified").default(System.currentTimeMillis())
    val ownerId = varchar("owner_id", 50) // ID владельца проекта

    override val primaryKey = PrimaryKey(ParticipantTable.id)
}