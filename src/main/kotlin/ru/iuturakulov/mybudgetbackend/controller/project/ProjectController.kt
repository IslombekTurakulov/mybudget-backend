package ru.iuturakulov.mybudgetbackend.controller.project

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.controller.user.DateTimeProvider
import ru.iuturakulov.mybudgetbackend.controller.user.SystemDateTimeProvider
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectStatus
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.models.InviteResult
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.ChangeRoleRequest
import ru.iuturakulov.mybudgetbackend.models.project.CreateProjectRequest
import ru.iuturakulov.mybudgetbackend.models.fcm.InvitationPreferencesRequest
import ru.iuturakulov.mybudgetbackend.models.fcm.InvitationPreferencesResponse
import ru.iuturakulov.mybudgetbackend.models.fcm.NotificationContext
import ru.iuturakulov.mybudgetbackend.models.project.InviteParticipantRequest
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest
import ru.iuturakulov.mybudgetbackend.repositories.FCMNotificationTableRepository
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.InvitationService
import ru.iuturakulov.mybudgetbackend.services.NotificationManager
import java.util.*

class ProjectController(
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl,
    private val invitationService: InvitationService,
    private val fcmNotificationTableRepository: FCMNotificationTableRepository,
    private val auditLogService: AuditLogService,
    private val notificationManager: NotificationManager,
    private val clock: DateTimeProvider = SystemDateTimeProvider()
) {

    /**
     * Получить список проектов пользователя
     */
    fun getUserProjects(userId: String): List<ProjectEntity> {
        return projectRepository.getProjectsByUser(userId).map { project ->
            if (project.ownerId == userId) {
                project.copy(ownerName = "Вы")
            } else {
                project
            }
        }
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
        return transaction {
            request.validation()

            val user = UserRepository().getUserById(ownerId)
                ?: throw AppException.NotFound.User("Пользователь не найден")

            val generatedProjectId = UUID.randomUUID().toString()

            ProjectsTable.insert { statement ->
                statement[id] = generatedProjectId
                statement[name] = request.name
                statement[description] = request.description
                statement[budgetLimit] = request.budgetLimit.toBigDecimal()
                statement[amountSpent] = 0.toBigDecimal()
                statement[categoryIcon] = request.categoryIcon
                statement[category] = request.category
                statement[status] = ProjectStatus.ACTIVE
                statement[createdAt] = clock.now().toEpochMilli()
                statement[lastModified] = clock.now().toEpochMilli()
                statement[ProjectsTable.ownerId] = ownerId
            }

            ParticipantTable.insert { insertStatement ->
                insertStatement[id] = UUID.randomUUID().toString()
                insertStatement[projectId] = generatedProjectId
                insertStatement[userId] = ownerId
                insertStatement[name] = user.name
                insertStatement[email] = user.email
                insertStatement[role] = UserRole.OWNER
                insertStatement[createdAt] = clock.now().toEpochMilli()
            }

            auditLogService.logAction(ownerId, "Создан проект: $generatedProjectId")

            projectRepository.getProjectById(generatedProjectId)!!
        }
    }

    /**
     * Обновить проект (разрешено владельцу и редакторам)
     */
    fun updateProject(
        userId: String,
        projectId: String,
        request: UpdateProjectRequest
    ): ProjectEntity = transaction {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project()

        // восстановление из архива
        if (project.status == ProjectStatus.ARCHIVED &&
            request.status == ProjectStatus.ACTIVE
        ) {
            return@transaction unarchiveProject(userId, projectId)
        }

        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.Authorization("Вы не участник проекта")

        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Вы не можете редактировать этот проект")
        }

        request.budgetLimit?.let { newLimit ->
            if (newLimit < project.amountSpent) {
                throw AppException.InvalidProperty.Project(
                    "Новый лимит (${newLimit.formatMoney()}) меньше уже потраченной суммы (${project.amountSpent.formatMoney()})"
                )
            }
        }

        val updated = projectRepository.updateProject(projectId, request)

        auditLogService.logAction(userId, "Обновил проект $projectId")

        notificationManager.sendNotification(
            type = NotificationType.PROJECT_EDITED,
            ctx = NotificationContext(
                actor = participant.name,
                actorId = participant.userId,
                projectId = project.id,
                projectName = project.name
            )
        )
        updated
    }

    private fun Double.formatMoney() = "%,.2f ₽".format(this)

    /**
     * Архивировать проект (только владелец)
     */
    fun archiveProject(userId: String, projectId: String, request: UpdateProjectRequest) = transaction {
        val project = (ProjectsTable innerJoin UserTable).selectAll().where { ProjectsTable.id eq projectId }
            .map { ProjectEntity.fromRow(it) }
            .singleOrNull() ?: throw AppException.NotFound.Project("Проект не найден")

        if (project.ownerId != userId) {
            throw AppException.Authorization("Только владелец может архивировать проект")
        }

        val owner = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.NotFound.User("Владелец не найден")

        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[status] = ProjectStatus.ARCHIVED
        }

        projectRepository.updateProject(projectId, request)

        auditLogService.logAction(userId, "Архивировал проект: $projectId")

        notificationManager.sendNotification(
            type = NotificationType.PARTICIPANT_ROLE_CHANGE,
            ctx = NotificationContext(
                actor = owner.name,
                actorId = owner.userId,
                projectId = project.id,
                projectName = project.name
            )
        )

        project.copy(
            status = ProjectStatus.ARCHIVED
        )
    }

    fun unarchiveProject(userId: String, projectId: String) = transaction {
        val project = (ProjectsTable innerJoin UserTable).selectAll().where { ProjectsTable.id eq projectId }
            .map { ProjectEntity.fromRow(it) }
            .singleOrNull() ?: throw AppException.NotFound.Project("Проект не найден")

        if (project.ownerId != userId) {
            throw AppException.Authorization("Только владелец может разархивировать проект")
        }

        val owner = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.NotFound.User("Владелец не найден")

        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[status] = ProjectStatus.ACTIVE
        }

        auditLogService.logAction(userId, "Зарархивировал проект: $projectId")

        notificationManager.sendNotification(
            type = NotificationType.PROJECT_ARCHIVED,
            ctx = NotificationContext(
                actor = owner.name,
                actorId = owner.userId,
                projectId = project.id,
                projectName = project.name
            )
        )

        project.copy(
            status = ProjectStatus.ACTIVE
        )
    }

    /**
     * Удалить проект (только владелец)
     */
    fun deleteProject(userId: String, projectId: String, deleteForever: Boolean = false) = transaction {
        val project = (ProjectsTable innerJoin UserTable).selectAll().where { ProjectsTable.id eq projectId }
            .map { ProjectEntity.fromRow(it) }
            .singleOrNull() ?: throw AppException.NotFound.Project("Проект не найден")

        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.NotFound.User("Участник не найден")

        if (project.ownerId != userId) {
            throw AppException.Authorization("Только владелец может удалить проект")
        }

        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[status] = ProjectStatus.DELETED
        }

        auditLogService.logAction(userId, "Удалил проект: $projectId")

        notificationManager.sendNotification(
            type = NotificationType.PROJECT_REMOVED,
            ctx = NotificationContext(
                actor = participant.name,
                actorId = participant.userId,
                projectId = project.id,
                projectName = project.name
            )
        )
    }


    /**
     * Пригласить участника
     */
    fun inviteParticipant(
        userId: String,
        projectId: String,
        request: InviteParticipantRequest
    ): InviteResult {
        val project = projectRepository.getProjectById(projectId)
            ?: return InviteResult(false, "Проект не найден")

        val inviter = UserRepository().getUserById(userId)
            ?: return InviteResult(false, "Приглашающий не найден")

        if (!projectRepository.isUserOwner(projectId, userId)) {
            return InviteResult(false, "Только владелец может приглашать участников")
        }

        request.email?.let {
            if (invitationService.hasRecentInvitation(it, projectId)) {
                return InviteResult(false, "Слишком много приглашений, попробуйте позже")
            }
        }

        val participant = request.email?.let { participantRepository.getParticipantByEmailAndProjectId(it, projectId) }

        if (participant != null) {
            return InviteResult(true, "Пользователь \"${participant.name}, ${participant.email}\" уже существует в проекте!")
        }

        // Генерим код и сохраняем приглашение в БД
        val inviteCode = invitationService.generateInvitation(
            projectId = projectId,
            email = if (request.type == InviteParticipantRequest.InvitationType.MANUAL) request.email else null,
            role = request.role
        )

        auditLogService.logAction(userId, "Создано приглашение $inviteCode для проекта $projectId")
        if (request.type == InviteParticipantRequest.InvitationType.MANUAL) {
            val user = UserRepository().getUserByEmail(request.email!!)
                ?: throw AppException.NotFound.User("Пользователь не найден")

            notificationManager.sendNotification(
                type = NotificationType.PROJECT_INVITE_SEND,
                ctx = NotificationContext(
                    actor = inviter.name,
                    actorId = inviter.id,
                    projectId = project.id,
                    projectName = project.name
                ),
                recipients = listOf(user.id)
            )

            invitationService.sendInvitationEmail(request.email, inviteCode, project.name)
            return InviteResult(true, "Приглашение отправлено на почту ${request.email}")
        } else {
            // QR-тип — возвращаем Base64-строку PNG-изображения
            val qrBase64 = invitationService.generateQrCodeBase64(inviteCode)
            return InviteResult(
                success = true,
                message = "Сгенерирован QR-код для приглашения",
                qrCodeBase64 = qrBase64,
                inviteCode = inviteCode
            )
        }
    }

    /**
     * Принять приглашение
     */
    fun acceptInvitation(userId: String, inviteCode: String): Boolean {
        return transaction {
            val invitation = invitationService.getInvitation(inviteCode)
                ?: throw AppException.NotFound.Resource("Приглашения не существует")

            // Проверяем, не истекло ли приглашение если прошло больше 24 часов
            if (invitation.isExpired()) {
                invitationService.deleteInvitation(invitation.id)
                throw AppException.ActionNotAllowed("Время приглашения истекло")
            }

            val user = UserRepository().getUserById(userId)
                ?: throw AppException.NotFound.User("Пользователь не найден")

            val participantRequest = participantRepository.getParticipantByUserAndProjectId(userId, invitation.projectId)

            if (participantRequest != null) {
                throw AppException.AlreadyExists.User("Пользователь уже в проекте")
            }

            // Добавляем участника в проект
            participantRepository.addParticipant(
                ParticipantEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = invitation.projectId,
                    userId = userId,
                    name = user.name,
                    email = user.email,
                    role = invitation.role,
                    createdAt = System.currentTimeMillis()
                )
            )

            invitationService.deleteInvitation(invitation.id)

            auditLogService.logAction(userId, "Принял приглашение в проект ${invitation.projectId}")

            // Отправляем уведомление владельцу проекта
            val project = projectRepository.getProjectById(invitation.projectId)
                ?: throw AppException.NotFound.Project("Проект не найден")

            notificationManager.sendNotification(
                type = NotificationType.PROJECT_INVITE_ACCEPT,
                ctx = NotificationContext(
                    actor = user.name,
                    actorId = user.id,
                    projectId = project.id,
                    projectName = project.name
                ),
                recipients = listOf(project.ownerId)
            )
            return@transaction true
        }
    }

    /**
     * Изменить роль участника (только владелец)
     */
    fun changeParticipantRole(userId: String, projectId: String, request: ChangeRoleRequest): Boolean {
        return transaction {
            val project = projectRepository.getProjectById(projectId) ?: return@transaction false

            if (!projectRepository.isUserOwner(projectId, userId)) {
                throw AppException.Authorization("Только владелец может изменять роли участников")
            }

            val owner = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
                ?: throw AppException.NotFound.User("Владелец не найден")

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

            notificationManager.sendNotification(
                type = NotificationType.PARTICIPANT_ROLE_CHANGE,
                ctx = NotificationContext(
                    actor = owner.name,
                    actorId = owner.userId,
                    projectId = project.id,
                    projectName = project.name
                ),
                recipients = listOf(request.userId)
            )
            return@transaction true
        }
    }

    fun getCurrentUserProjectRole(userId: String, projectId: String): ParticipantEntity {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        if (!projectRepository.isUserParticipant(userId, projectId)) {
            throw AppException.Authorization("Вы не участник проекта")
        }

        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
            ?: throw AppException.NotFound.User("Участник не найден")

        return participant
    }

    fun getProjectParticipants(userId: String, projectId: String): List<ParticipantEntity> {
        val project = projectRepository.getProjectById(projectId)
            ?: throw AppException.NotFound.Project("Проект не найден")

        if (!projectRepository.isUserParticipant(userId, projectId)) {
            throw AppException.Authorization("Вы не участник проекта")
        }

        return participantRepository.getParticipantsByProject(projectId)
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

            val owner = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
                ?: throw AppException.NotFound.User("Владелец не найден")

            participantRepository.removeParticipant(participantId = participantId, projectId = projectId)

            auditLogService.logAction(userId, "Удален участник $participantId из проекта $projectId")

            notificationManager.sendNotification(
                type = NotificationType.PARTICIPANT_REMOVED,
                ctx = NotificationContext(
                    actor = owner.name,
                    actorId = owner.userId,
                    projectId = project.id,
                    projectName = project.name
                ),
                recipients = listOf(participantId)
            )
        }
    }

    fun getNotificationPreferencesParticipant(userId: String, projectId: String): InvitationPreferencesResponse {
        return transaction {
            val project = projectRepository.getProjectById(projectId)
                ?: throw AppException.NotFound.Project()

            val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
                ?: throw AppException.NotFound.User("Участник не найден")

            val types = fcmNotificationTableRepository.find(userId, projectId)
            InvitationPreferencesResponse(types)
        }
    }

    fun setNotificationPreferencesToParticipant(userId: String, projectId: String, request: InvitationPreferencesRequest) {
        return transaction {
            val project = projectRepository.getProjectById(projectId)
                ?: throw AppException.NotFound.Project()

            val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)
                ?: throw AppException.NotFound.User("Участник не найден")

            fcmNotificationTableRepository.upsert(
                userId = userId,
                projectId = projectId,
                types = request.types
            )
        }
    }
}
