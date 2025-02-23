package ru.iuturakulov.mybudgetbackend.controller.project

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.user.UserEntity
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.models.InviteResult
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.AcceptInviteRequest
import ru.iuturakulov.mybudgetbackend.models.project.ChangeRoleRequest
import ru.iuturakulov.mybudgetbackend.models.project.CreateProjectRequest
import ru.iuturakulov.mybudgetbackend.models.project.InviteParticipantRequest
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import services.InvitationService
import services.NotificationService
import java.util.*

class ProjectController(
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl,
    private val invitationService: InvitationService,
    private val notificationService: NotificationService,
    private val auditLogService: AuditLogService
) {

    /**
     * Получить список проектов пользователя
     */
    fun getUserProjects(userId: String): List<ProjectEntity> {
        return projectRepository.getProjectsByUser(userId)
    }

    /**
     * Получить проект по ID (только участникам)
     */
    fun getProjectById(userId: String, projectId: String): ProjectEntity {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
        if (!accessControl.canViewProject(userId, project, participant)) {
            throw AppException.Authorization("У вас нет доступа к этому проекту")
        }

        return project
    }

    /**
     * Создать проект (создатель становится владельцем)
     */
    fun createProject(ownerId: String, request: CreateProjectRequest): ProjectEntity {
        request.validation()

        return projectRepository.createProject(ownerId, request).also { project ->
            val projectId = project.id ?: throw AppException.NotFound.Project("Проект не создан")

            // Загружаем информацию о пользователе
            val user = transaction {
                UserTable.selectAll().where { UserTable.id eq ownerId }
                    .map { row -> UserEntity.fromRow(row) }
                    .singleOrNull()
            } ?: throw AppException.NotFound.User("Пользователь не найден")

            // Добавляем владельца как участника проекта
            participantRepository.addParticipant(
                ParticipantEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    userId = ownerId,
                    name = user.name,
                    email = user.email,
                    role = UserRole.OWNER,
                    createdAt = System.currentTimeMillis()
                )
            )

            auditLogService.logAction(ownerId, "Создан проект: ${project.id}")
        }
    }

    /**
     * Обновить проект (разрешено владельцу и редакторам)
     */
    fun updateProject(userId: String, projectId: String, request: UpdateProjectRequest): ProjectEntity {
        val project = projectRepository.getProjectById(projectId) ?: throw AppException.NotFound.Project()
        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)

        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Вы не можете редактировать этот проект")
        }

        return projectRepository.updateProject(projectId, request).also {
            auditLogService.logAction(userId, "Обновлен проект: $projectId")
        }
    }

    /**
     * Удалить проект (только владелец)
     */
    fun deleteProject(userId: String, projectId: String) {
        val project = projectRepository.getProjectById(projectId) ?: throw AppException.NotFound.Project()

        if (!accessControl.canDeleteProject(userId, project)) {
            throw AppException.Authorization("Только владелец может удалить проект")
        }

        projectRepository.deleteProject(projectId)
        auditLogService.logAction(userId, "Удален проект: $projectId")
    }

    /**
     * Пригласить участника
     */
    fun inviteParticipant(userId: String, projectId: String, request: InviteParticipantRequest): InviteResult {
        return transaction {
            projectRepository.getProjectById(projectId)
                ?: return@transaction InviteResult(success = false, message = "Проект не найден")

            if (!projectRepository.isUserOwner(projectId, userId)) {
                return@transaction InviteResult(
                    success = false,
                    message = "Только владелец может приглашать участников"
                )
            }

            if (invitationService.hasRecentInvitation(request.email, projectId)) {
                return@transaction InviteResult(
                    success = false,
                    message = "Слишком много приглашений, попробуйте позже"
                )
            }

            val inviteCode = invitationService.generateInvitation(projectId, request.email, request.role)
            invitationService.sendInvitationEmail(request.email, inviteCode)

            auditLogService.logAction(userId, "Приглашен ${request.email} в проект $projectId")

            // Отправляем уведомление
            notificationService.sendNotification(
                userId = request.email,
                type = NotificationType.PROJECT_INVITE,
                message = "Вас пригласили в проект $projectId",
                projectId = projectId
            )

            return@transaction InviteResult(
                success = true,
                message = "Приглашение отправлено"
            )
        }
    }

    /**
     * Принять приглашение
     */
    fun acceptInvitation(userId: String, request: AcceptInviteRequest): Boolean {
        return transaction {
            val invitation = invitationService.getInvitation(request.projectId, request.inviteCode)
                ?: return@transaction false

            // Проверяем, не истекло ли приглашение
            if (invitation.isExpired()) {
                invitationService.deleteInvitation(invitation.id)
                return@transaction false
            }

            val user = transaction {
                UserTable.selectAll().where { UserTable.id eq userId }
                    .map { row -> UserEntity.fromRow(row) }
                    .singleOrNull()
            } ?: throw AppException.NotFound.User("Пользователь не найден")

            // Добавляем участника в проект
            participantRepository.addParticipant(
                ParticipantEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = request.projectId,
                    userId = userId,
                    name = user.name,
                    email = user.email,
                    role = invitation.role,
                    createdAt = System.currentTimeMillis()
                )
            )

            invitationService.deleteInvitation(invitation.id)

            auditLogService.logAction(userId, "Принял приглашение в проект ${request.projectId}")

            // Отправляем уведомление владельцу проекта
            val project = projectRepository.getProjectById(request.projectId)
                ?: throw AppException.NotFound.Project("Проект не найден")

            notificationService.sendNotification(
                userId = project.ownerId,
                type = NotificationType.PROJECT_INVITE,
                message = "Пользователь ${user.email} принял приглашение в проект $request.projectId",
                projectId = request.projectId
            )
            return@transaction true
        }
    }

    /**
     * Изменить роль участника (только владелец)
     */
    fun changeParticipantRole(userId: String, projectId: String, request: ChangeRoleRequest): Boolean {
        return transaction {
            projectRepository.getProjectById(projectId) ?: return@transaction false

            if (!projectRepository.isUserOwner(projectId, userId)) {
                throw AppException.Authorization("Только владелец может изменять роли участников")
            }

            // Находим участника
            val participant = participantRepository.getParticipantByUserAndProjectId(request.userId, projectId)
                ?: throw AppException.NotFound.User("Участник не найден")

            val participantId = participant.id ?: throw AppException.NotFound.User("Участник не найден")

            // Обновляем роль
            participantRepository.updateParticipantRole(participantId, request.role)

            auditLogService.logAction(
                userId = userId,
                action = "Изменена роль пользователя ${request.userId} на ${request.role} в проекте $projectId"
            )

            notificationService.sendNotification(
                userId = request.userId,
                type = NotificationType.ROLE_CHANGE,
                message = "Ваша роль в проекте $projectId изменена на ${request.role}",
                projectId = projectId
            )
            return@transaction true
        }
    }


    /**
     * Удалить участника (только владелец или сам участник)
     */
    fun removeParticipant(userId: String, projectId: String, participantId: String) {
        return transaction {
            val project = projectRepository.getProjectById(projectId)
                ?: throw AppException.NotFound.Project()

            // Проверяем, есть ли такой участник
            val participant = participantRepository.getParticipantByUserAndProjectId(participantId, projectId)
                ?: throw AppException.NotFound.User("Участник не найден")

            // Проверяем, что проект всегда имеет владельца
            if (participant.role == UserRole.OWNER) {
                val ownersCount = participantRepository.getProjectOwnersCount(projectId)
                if (ownersCount <= 1) {
                    throw AppException.Authorization("Нельзя удалить последнего владельца проекта")
                }
            }

            // Проверяем права: владелец проекта или пользователь удаляет себя
            if (!projectRepository.isUserOwner(projectId, userId) && userId != participantId) {
                throw AppException.Authorization("Вы не можете удалить этого участника")
            }

            participantRepository.removeParticipant(participantId)

            auditLogService.logAction(userId, "Удален участник $participantId из проекта $projectId")

            notificationService.sendNotification(
                userId = participantId,
                type = NotificationType.SYSTEM_ALERT,
                message = "Вы были удалены из проекта $projectId",
                projectId = projectId
            )
        }
    }
}
