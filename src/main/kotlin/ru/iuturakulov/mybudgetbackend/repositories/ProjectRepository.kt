package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import java.util.*

class ProjectRepository {

    fun createProject(project: ProjectEntity): ProjectEntity = transaction {
        val projectId = UUID.randomUUID().toString()
        ProjectsTable.insert {
            it[id] = projectId
            it[name] = project.name
            it[description] = project.description
            it[budgetLimit] = project.budgetLimit.toBigDecimal()
            it[amountSpent] = project.amountSpent.toBigDecimal()
            it[status] = project.status
            it[createdAt] = project.createdAt
            it[lastModified] = project.lastModified
            it[ownerId] = project.ownerId
        }

        project.copy(id = projectId)
    }

    fun getProjectById(id: String): ProjectEntity? = transaction {
        ProjectsTable.selectAll().where { ProjectsTable.id eq id }
            .mapNotNull { ProjectEntity.fromRow(it) }
            .singleOrNull()
    }

    fun updateProject(project: ProjectEntity): Boolean = transaction {
        ProjectsTable.update({ ProjectsTable.id eq project.id!! }) {
            it[name] = project.name
            it[description] = project.description
            it[budgetLimit] = project.budgetLimit.toBigDecimal()
            it[amountSpent] = project.amountSpent.toBigDecimal()
            it[status] = project.status
            it[lastModified] = project.lastModified
        } > 0
    }

    fun deleteProject(id: String): Boolean = transaction {
        ProjectsTable.deleteWhere { ProjectsTable.id eq id } > 0
    }
}