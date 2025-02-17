package ru.iuturakulov.mybudgetbackend.controller.project

import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.models.project.ProjectCreateRequest
import ru.iuturakulov.mybudgetbackend.models.project.ProjectUpdateRequest
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository
import ru.iuturakulov.mybudgetbackend.repositories.ProjectRepository

class ProjectController(
    private val projectRepository: ProjectRepository,
    private val participantRepository: ParticipantRepository,
    private val accessControl: AccessControl
) {

    /**
     * Получить список проектов, в которых участвует пользователь
     */
    fun getUserProjects(userId: String): List<ProjectEntity> {
        return projectRepository.getProjectsByUser(userId)
    }

    /**
     * Получить информацию о проекте (доступно только участникам)
     */
    fun getProjectById(userId: String, projectId: String): ProjectEntity? {
        val project = projectRepository.getProjectById(projectId) ?: throw AppException.NotFound.Project()
        val participant = participantRepository.getParticipantByUserAndProjectId(userId, projectId)

        if (!accessControl.canViewProject(userId, project, participant)) {
            throw AppException.Authorization("У вас нет доступа к этому проекту")
        }

        return project
    }

    /**
     * Получить информацию об участнике проекта
     */
    fun getParticipant(userId: String, projectId: String): ParticipantEntity? {
        return participantRepository.getParticipantByUserAndProjectId(userId, projectId)
    }

    /**
     * Создать новый проект (пользователь автоматически становится владельцем)
     */
    fun createProject(ownerId: String, request: CreateProjectRequest): ProjectEntity {
        request.validation()

        return projectRepository.createProject(ownerId, request).also { project ->
            participantRepository.addParticipant(
                projectId = project.id,
                userId = ownerId,
                role = UserRole.OWNER
            )
        }
    }

    /**
     * Обновить проект (разрешено только владельцу и редакторам)
     */
    fun updateProject(userId: String, projectId: String, request: UpdateProjectRequest): ProjectEntity {
        val project = projectRepository.getProjectById(projectId) ?: throw AppException.NotFound.Project()
        val participant = projectRepository.getParticipant(userId, projectId)

        if (!accessControl.canEditProject(userId, project, participant)) {
            throw AppException.Authorization("Вы не можете редактировать этот проект")
        }

        return projectRepository.updateProject(projectId, request)
    }

    /**
     * Удалить проект (разрешено только владельцу)
     */
    fun deleteProject(userId: String, projectId: String) {
        val project = projectRepository.getProjectById(projectId) ?: throw AppException.NotFound.Project()

        if (!accessControl.canDeleteProject(userId, project)) {
            throw AppException.Authorization("Только владелец может удалить этот проект")
        }

        projectRepository.deleteProject(projectId)
    }
}
