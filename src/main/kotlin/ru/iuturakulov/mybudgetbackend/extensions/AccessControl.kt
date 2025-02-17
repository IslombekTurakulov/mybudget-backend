package ru.iuturakulov.mybudgetbackend.extensions

import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository

object AccessControl {

    fun canEditProject(userId: String, project: ProjectEntity, participant: ParticipantEntity?): Boolean {
        return project.ownerId == userId || participant?.role == UserRole.EDITOR
    }

    fun canViewProject(userId: String, project: ProjectEntity, participant: ParticipantEntity?): Boolean {
        return project.ownerId == userId || participant != null
    }

    fun canDeleteProject(userId: String, project: ProjectEntity): Boolean {
        return project.ownerId == userId
    }

    /**
     * Проверяет, имеет ли пользователь право на выполнение действия в рамках проекта
     */
    fun checkAccess(
        userId: String,
        projectId: String,
        allowedRoles: Set<UserRole>,
        participantRepository: ParticipantRepository
    ) {
        val userRole = participantRepository.getUserRoleInProject(userId, projectId)
            ?: throw AppException.Authorization("User has no access to this project")

        if (!allowedRoles.contains(userRole)) {
            throw AppException.Authorization("Access denied: Insufficient permissions")
        }
    }
}
