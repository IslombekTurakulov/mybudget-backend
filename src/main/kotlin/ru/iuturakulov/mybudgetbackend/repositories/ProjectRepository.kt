package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectStatus
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.CreateProjectRequest
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest
import java.util.*

class ProjectRepository {

    /**
     * Создать новый проект
     */
    fun createProject(ownerId: String, request: CreateProjectRequest): ProjectEntity = transaction {
        val generatedProjectId = UUID.randomUUID().toString()

        ProjectsTable.insert { statement ->
            statement[id] = generatedProjectId
            statement[name] = request.name
            statement[description] = request.description
            statement[budgetLimit] = request.budgetLimit.toBigDecimal()
            statement[amountSpent] = 0.toBigDecimal()
            statement[status] = ProjectStatus.ACTIVE
            statement[createdAt] = System.currentTimeMillis()
            statement[lastModified] = System.currentTimeMillis()
            statement[ProjectsTable.ownerId] = ownerId
        }

        // Добавляем владельца в участники проекта
        ParticipantTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[projectId] = generatedProjectId
            it[userId] = ownerId
            it[role] = UserRole.OWNER
            it[createdAt] = System.currentTimeMillis()
        }

        getProjectById(generatedProjectId) ?: throw AppException.NotFound.Project()
    }

    /**
     * Получить проект по ID
     */
    fun getProjectById(id: String): ProjectEntity? = transaction {
        ProjectsTable.selectAll().where { ProjectsTable.id eq id }
            .mapNotNull { ProjectsTable.fromRow(it) }
            .singleOrNull()
    }

    /**
     * Получить проекты, в которых участвует пользователь
     */
    fun getProjectsByUser(userId: String): List<ProjectEntity> = transaction {
        (ProjectsTable innerJoin ParticipantTable)
            .selectAll().where { ParticipantTable.userId eq userId }
            .map { ProjectsTable.fromRow(it) }
    }

    /**
     * Проверить, является ли пользователь владельцем проекта
     */
    fun isUserOwner(projectId: String, userId: String): Boolean = transaction {
        ParticipantTable.selectAll().where {
            (ParticipantTable.projectId eq projectId) and
                    (ParticipantTable.userId eq userId) and
                    (ParticipantTable.role eq UserRole.OWNER)
        }.count() > 0
    }

    /**
     * Обновить данные проекта (разрешено только владельцу и редакторам)
     */
    fun updateProject(projectId: String, request: UpdateProjectRequest): ProjectEntity = transaction {
        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            request.name?.let { requestName ->
                it[name] = requestName
            }
            request.description?.let { requestDescription ->
                it[description] = requestDescription
            }
            request.budgetLimit?.let { requestBudgetLimit ->
                it[budgetLimit] = requestBudgetLimit.toBigDecimal()
            }
            it[lastModified] = System.currentTimeMillis()
        }

        getProjectById(projectId) ?: throw AppException.NotFound.Project()
    }

    /**
     * Удалить проект (только владелец)
     */
    fun deleteProject(projectId: String) = transaction {
        // Удаляем участников проекта
        ParticipantTable.deleteWhere { ParticipantTable.projectId eq projectId }

        // Удаляем сам проект
        ProjectsTable.deleteWhere { id eq projectId }
    }

    fun isUserParticipant(userId: String, projectId: String): Boolean = transaction {
        ParticipantTable.selectAll().where {
            (ParticipantTable.userId eq userId) and (ParticipantTable.projectId eq projectId)
        }.count() > 0
    }

}
