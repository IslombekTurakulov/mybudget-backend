package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest

class ProjectRepository {


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

    fun updateAmountSpent(projectId: String, newAmountSpent: Double) = transaction {
        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[amountSpent] = newAmountSpent.toBigDecimal()
        }
    }

    fun updateBudgetAmount(projectId: String, newBudgetAmount: Double) = transaction {
        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[budgetLimit] = newBudgetAmount.toBigDecimal()
        }
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

    fun isUserParticipant(userId: String, projectId: String): Boolean = transaction {
        ParticipantTable.selectAll().where {
            (ParticipantTable.userId eq userId) and (ParticipantTable.projectId eq projectId)
        }.count() > 0
    }

}
