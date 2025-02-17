package ru.iuturakulov.mybudgetbackend.controller.participant

import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.models.UserRole
import ru.iuturakulov.mybudgetbackend.repositories.ParticipantRepository

class ParticipantController(private val participantRepository: ParticipantRepository) {

    fun getProjectParticipants(projectId: String): List<ParticipantEntity> {
        return participantRepository.getParticipantsByProject(projectId)
    }

    fun addParticipant(request: AddParticipantRequest): ParticipantEntity {
        val participant = ParticipantEntity(
            projectId = request.projectId,
            userId = request.userId,
            name = request.name,
            email = request.email,
            role = request.role
        )
        participantRepository.addParticipant(participant)
        return participant
    }

    fun removeParticipant(participantId: String): Boolean {
        return participantRepository.removeParticipant(participantId)
    }

    fun updateParticipantRole(participantId: String, newRole: UserRole): Boolean {
        return participantRepository.updateParticipantRole(participantId, newRole)
    }
}
