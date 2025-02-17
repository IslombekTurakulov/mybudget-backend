package ru.iuturakulov.mybudgetbackend.repositories

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantEntity
import ru.iuturakulov.mybudgetbackend.entities.participants.ParticipantTable
import ru.iuturakulov.mybudgetbackend.models.UserRole

class ParticipantRepository {

    fun getParticipantsByProject(projectId: String): List<ParticipantEntity> = transaction {
        ParticipantTable.selectAll().where { ParticipantTable.projectId eq projectId }
            .map { ParticipantTable.fromRow(it) }
    }

    fun getParticipantByUserAndProjectId(userId: String, projectId: String): ParticipantEntity? = transaction {
        ParticipantTable.selectAll().where {
            (ParticipantTable.userId eq userId) and (ParticipantTable.projectId eq projectId)
        }.map { ParticipantTable.fromRow(it) }.firstOrNull()
    }

    fun addParticipant(participant: ParticipantEntity) = transaction {
        ParticipantTable.insert {
            it[id] = participant.id
            it[projectId] = participant.projectId
            it[userId] = participant.userId
            it[name] = participant.name
            it[email] = participant.email
            it[role] = participant.role
            it[createdAt] = participant.createdAt
        }
    }

    fun removeParticipant(participantId: String): Boolean = transaction {
        ParticipantTable.deleteWhere { ParticipantTable.id eq participantId } > 0
    }

    fun updateParticipantRole(participantId: String, newRole: UserRole): Boolean = transaction {
        ParticipantTable.update({ ParticipantTable.id eq participantId }) {
            it[role] = newRole
        } > 0
    }
}
