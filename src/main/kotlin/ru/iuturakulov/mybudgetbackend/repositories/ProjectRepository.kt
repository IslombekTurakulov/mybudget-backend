package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.controller.user.DateTimeProvider
import ru.iuturakulov.mybudgetbackend.controller.user.SystemDateTimeProvider
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest
import java.math.BigDecimal

class ProjectRepository(
    private val clock: DateTimeProvider = SystemDateTimeProvider()
) {

    /**
     * Получить проект по ID (включая данные владельца)
     */
    fun getProjectById(id: String): ProjectEntity? = transaction {
        (ProjectsTable innerJoin UserTable)
            .selectAll().where { ProjectsTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { ProjectsTable.fromRow(it) }
    }

    /**
     * Получить проекты, в которых участвует пользователь (включая данные владельца)
     */
    fun getProjectsByUser(userId: String): List<ProjectEntity> = transaction {
        // Сначала join с участниками, затем с таблицей пользователей для получения владельца
        val join = ProjectsTable
            .innerJoin(ParticipantTable, { ProjectsTable.id }, { ParticipantTable.projectId })
            .innerJoin(UserTable, { ProjectsTable.ownerId }, { UserTable.id })

        join.selectAll().where { ParticipantTable.userId eq userId }
            .map { ProjectsTable.fromRow(it) }
    }

    /**
     * Обновить сразу оба поля: amountSpent и/или budgetLimit
     */
    fun updateProjectAmounts(
        projectId: String,
        amountSpent: BigDecimal? = null,
        budgetLimit: BigDecimal? = null
    ) = transaction {
        val updated = ProjectsTable.update({ ProjectsTable.id eq projectId }) { stmt ->
            amountSpent?.let { stmt[ProjectsTable.amountSpent] = it }
            budgetLimit?.let { stmt[ProjectsTable.budgetLimit] = it }
            stmt[ProjectsTable.lastModified] = clock.now().toEpochMilli()
        }
        if (updated == 0) {
            throw AppException.NotFound.Project("Проект с id=$projectId не найден")
        }
    }

    /**
     * Проверить, является ли пользователь владельцем проекта
     */
    fun isUserOwner(projectId: String, userId: String): Boolean = transaction {
        ParticipantTable
            .selectAll().where {
                (ParticipantTable.projectId eq projectId) and
                        (ParticipantTable.userId eq userId) and
                        (ParticipantTable.role eq UserRole.OWNER)
            }
            .any()
    }

    /**
     * Проверить, является ли пользователь участником проекта
     */
    fun isUserParticipant(userId: String, projectId: String): Boolean = transaction {
        ParticipantTable
            .selectAll().where {
                (ParticipantTable.userId eq userId) and
                        (ParticipantTable.projectId eq projectId)
            }
            .any()
    }

    /**
     * Обновить данные проекта (имя, описание, бюджет, категория и иконка) -- только для владельца/редактора
     */
    fun updateProject(projectId: String, request: UpdateProjectRequest): ProjectEntity = transaction {
        val updated = ProjectsTable.update({ ProjectsTable.id eq projectId }) { stmt ->
            request.name?.let { stmt[ProjectsTable.name] = it }
            request.description?.let { stmt[ProjectsTable.description] = it }
            request.budgetLimit?.let { stmt[ProjectsTable.budgetLimit] = it.toBigDecimal() }
            request.category?.let { stmt[ProjectsTable.category] = it }
            request.categoryIcon?.let { stmt[ProjectsTable.categoryIcon] = it }
            stmt[ProjectsTable.lastModified] = clock.now().toEpochMilli()
        }
        if (updated == 0) {
            throw AppException.NotFound.Project("Проект с id=$projectId не найден")
        }
        getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект с id=$projectId не найден после обновления")
    }
}
