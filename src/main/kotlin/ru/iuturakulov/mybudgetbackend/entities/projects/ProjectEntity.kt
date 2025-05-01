package ru.iuturakulov.mybudgetbackend.entities.projects

import org.jetbrains.exposed.sql.ResultRow
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable

data class ProjectEntity(
    val id: String,
    val name: String,
    val description: String?,
    val budgetLimit: Double,
    val amountSpent: Double,
    val status: ProjectStatus,
    val createdAt: Long,
    val lastModified: Long,
    val ownerId: String,
    val ownerName: String,
    val ownerEmail: String,
    val category: String?,
    val categoryIcon: String?
) {
    companion object {
        fun fromRow(row: ResultRow) = ProjectsTable.fromRow(row)
    }
}