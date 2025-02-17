package ru.iuturakulov.mybudgetbackend.entities.projects

import org.jetbrains.exposed.sql.ResultRow

data class ProjectEntity(
    val id: String?,
    val name: String,
    val description: String?,
    val budgetLimit: Double,
    val amountSpent: Double,
    val status: ProjectStatus,
    val createdAt: Long,
    val lastModified: Long,
    val ownerId: String
) {
    companion object {
        fun fromRow(row: ResultRow) = ProjectEntity(
            id = row[ProjectsTable.id],
            name = row[ProjectsTable.name],
            description = row[ProjectsTable.description],
            budgetLimit = row[ProjectsTable.budgetLimit].toDouble(),
            amountSpent = row[ProjectsTable.amountSpent].toDouble(),
            status = row[ProjectsTable.status],
            createdAt = row[ProjectsTable.createdAt],
            lastModified = row[ProjectsTable.lastModified],
            ownerId = row[ProjectsTable.ownerId]
        )
    }
}