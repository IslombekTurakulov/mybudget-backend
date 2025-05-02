package ru.iuturakulov.mybudgetbackend.controller.project

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.controller.user.DateTimeProvider
import ru.iuturakulov.mybudgetbackend.controller.user.SystemDateTimeProvider
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationTable
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectStatus
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectsTable
import ru.iuturakulov.mybudgetbackend.entities.transaction.TransactionsTable
import ru.iuturakulov.mybudgetbackend.entities.user.UserTable
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.extensions.BudgetUtils.maybeNotifyLimit
import ru.iuturakulov.mybudgetbackend.models.InviteResult
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.ChangeRoleRequest
import ru.iuturakulov.mybudgetbackend.models.project.CreateProjectRequest
import ru.iuturakulov.mybudgetbackend.models.project.InviteParticipantRequest
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository
import ru.iuturakulov.mybudgetbackend.repositories.UserRepository
import ru.iuturakulov.mybudgetbackend.services.InvitationService
import ru.iuturakulov.mybudgetbackend.services.NotificationService
import java.util.*

class ProjectController(
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl,
    private val invitationService: InvitationService,
    private val notificationService: NotificationService,
    private val auditLogService: AuditLogService,
    private val clock: DateTimeProvider = SystemDateTimeProvider()
) {

    /**
     * Получить список проектов пользователя
     */
    fun getUserProjects(userId: String): List<ProjectEntity> {
        return projectRepository.getProjectsByUser(userId).sortedBy { project ->
            project.createdAt
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

        val participant = participantRepository
            .getParticipantByUserAndProjectId(userId, projectId)

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

        // оповещение при достижении 90%
        request.budgetLimit?.let {
            maybeNotifyLimit(
                project = updated,
                amountSpent = updated.amountSpent,
                participantRepo = participantRepository,
                notificationService = notificationService
            )
        }

        auditLogService.logAction(userId, "Обновил проект $projectId")
        updated
    }

    private fun Double.formatMoney() = "%,.2f ₽".format(this)

    /**
     * Архивировать проект (только владелец)
     */
    fun archiveProject(userId: String, projectId: String) = transaction {
        val project = (ProjectsTable innerJoin UserTable).selectAll().where { ProjectsTable.id eq projectId }
            .map { ProjectEntity.fromRow(it) }
            .singleOrNull() ?: throw AppException.NotFound.Project("Проект не найден")

        if (project.ownerId != userId) {
            throw AppException.Authorization("Только владелец может архивировать проект")
        }

        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[status] = ProjectStatus.ARCHIVED
        }

        auditLogService.logAction(userId, "Архивировал проект: $projectId")

        val participants = ParticipantTable.selectAll().where { ParticipantTable.projectId eq projectId }
            .mapNotNull { it[ParticipantTable.userId] }

        participants.forEach { participantId ->
            notificationService.sendNotification(
                userId = participantId,
                type = NotificationType.SYSTEM_ALERT,
                message = "Проект \"${project.name}\" был архивирован владельцем",
                projectId = projectId
            )
        }
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

        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[status] = ProjectStatus.ACTIVE
        }

        auditLogService.logAction(userId, "Зарархивировал проект: $projectId")

        val participants = ParticipantTable.selectAll().where { ParticipantTable.projectId eq projectId }
            .mapNotNull { it[ParticipantTable.userId] }

        participants.forEach { participantId ->
            notificationService.sendNotification(
                userId = participantId,
                type = NotificationType.SYSTEM_ALERT,
                message = "Проект \"${project.name}\" был разархивирован владельцем",
                projectId = projectId
            )
        }
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

        if (project.ownerId != userId) {
            throw AppException.Authorization("Только владелец может удалить проект")
        }

        ProjectsTable.update({ ProjectsTable.id eq projectId }) {
            it[status] = ProjectStatus.DELETED
        }

        auditLogService.logAction(userId, "Удалил проект: $projectId")

        val participants = ParticipantTable.selectAll().where { ParticipantTable.projectId eq projectId }
            .mapNotNull { it[ParticipantTable.userId] }
        participants.forEach { participantId ->
            notificationService.sendNotification(
                userId = participantId,
                type = NotificationType.SYSTEM_ALERT,
                message = "Проект \"${project.name}\" был удален владельцем",
                projectId = projectId
            )
        }
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

            notificationService.sendNotification(
                userId = user.id,
                type = NotificationType.PROJECT_INVITE,
                message = "Вас пригласили в проект \"${project.name}\"!. Для того, чтобы присоединиться к проекту, зайдите в вашу почту ${user.email} и поищите код приглашения.",
                projectId = projectId
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

            notificationService.sendNotification(
                userId = project.ownerId,
                type = NotificationType.PROJECT_INVITE,
                message = "Пользователь ${user.email} принял приглашение в проект \"${project.name}\"",
                projectId = invitation.projectId
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
                message = "Ваша роль в проекте \"${project.name}\" изменена на ${request.role}",
                projectId = projectId
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

            participantRepository.removeParticipant(participantId = participantId, projectId = projectId)

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
