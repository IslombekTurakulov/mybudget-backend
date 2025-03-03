package ru.iuturakulov.mybudgetbackend.extensions

import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.models.UserRole

class AccessControl {

    fun canEditProject(userId: String, project: ProjectEntity, participant: ParticipantEntity?): Boolean {
        return project.ownerId == userId || participant?.role == UserRole.EDITOR
    }

    fun canViewProject(userId: String, project: ProjectEntity, participant: ParticipantEntity?): Boolean {
        return project.ownerId == userId || participant != null
    }

    fun canDeleteProject(userId: String, project: ProjectEntity): Boolean {
        return project.ownerId == userId
    }
}
