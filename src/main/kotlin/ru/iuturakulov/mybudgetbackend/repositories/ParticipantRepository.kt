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
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.models.UserRole
import java.util.*

class ParticipantRepository {

    fun getParticipantsByProject(projectId: String): List<ParticipantEntity> = transaction {
        ParticipantTable.selectAll().where { ParticipantTable.projectId eq projectId }
            .map { ParticipantTable.fromRow(it) }
    }

    fun getParticipantByUserAndProjectId(userId: String, projectId: String): ParticipantEntity? = transaction {
        val participants = ParticipantTable.selectAll().where {
            (ParticipantTable.userId eq userId) and (ParticipantTable.projectId eq projectId)
        }
        return@transaction participants.map { ParticipantTable.fromRow(it) }.firstOrNull()
    }

    fun getParticipantByEmailAndProjectId(email: String, projectId: String): ParticipantEntity? = transaction {
        val participants = ParticipantTable.selectAll().where {
            (ParticipantTable.email eq email) and (ParticipantTable.projectId eq projectId)
        }
        return@transaction participants.map { ParticipantTable.fromRow(it) }.firstOrNull()
    }

    fun getProjectOwnerId(projectId: String) = transaction {
        ParticipantTable.selectAll().where {
            (ParticipantTable.projectId eq projectId) and (ParticipantTable.role eq UserRole.OWNER)
        }.map { ParticipantTable.fromRow(it) }.firstOrNull()
    }

    fun addParticipant(participant: ParticipantEntity) = transaction {
        ParticipantTable.insert { statement ->
            statement[id] = participant.id ?: UUID.randomUUID().toString() // Генерация ID
            statement[projectId] = participant.projectId
            statement[userId] = participant.userId
            statement[name] = participant.name
            statement[email] = participant.email
            statement[role] = participant.role
            statement[createdAt] = participant.createdAt
        }
    }

    fun getProjectOwnersCount(projectId: String): Long = transaction {
        ParticipantTable.selectAll().where {
            (ParticipantTable.projectId eq projectId) and
                    (ParticipantTable.role eq UserRole.OWNER)
        }.count()
    }

    fun removeParticipant(participantId: String, projectId: String): Boolean = transaction {
        val deletedRows = ParticipantTable.deleteWhere { (userId eq participantId) and (this.projectId eq projectId) }
        if (deletedRows == 0) {
            throw AppException.NotFound.User("Участник не найден")
        }
        true
    }

    fun updateParticipantRole(participantId: String, newRole: UserRole): Boolean = transaction {
        val exists = ParticipantTable.selectAll().where { ParticipantTable.id eq participantId }.count() > 0
        if (!exists) throw AppException.NotFound.User("Участник не найден")

        ParticipantTable.update({ ParticipantTable.id eq participantId }) {
            it[role] = newRole
        } > 0
    }

}
