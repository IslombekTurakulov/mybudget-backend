package ru.iuturakulov.mybudgetbackend.entities.projects

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object ProjectsTable : Table("projects") {
    val id = varchar("id", 36) // UUID проекта
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val budgetLimit = decimal("budget_limit", 12, 2)
    val amountSpent = decimal("amount_spent", 12, 2).default(0.0.toBigDecimal())
    val status = enumerationByName("status", 10, ProjectStatus::class)
    val createdAt = long("created_at").default(System.currentTimeMillis())
    val lastModified = long("last_modified").default(System.currentTimeMillis())
    val ownerId = varchar("owner_id", 50) // ID владельца проекта

    override val primaryKey = PrimaryKey(id)

    // Метод для преобразования строки из БД в `ProjectEntity`
    fun fromRow(row: ResultRow) = ProjectEntity(
        id = row[id],
        name = row[name],
        description = row[description],
        budgetLimit = row[budgetLimit].toDouble(),
        amountSpent = row[amountSpent].toDouble(),
        status = row[status],
        createdAt = row[createdAt],
        lastModified = row[lastModified],
        ownerId = row[ownerId]
    )
}
