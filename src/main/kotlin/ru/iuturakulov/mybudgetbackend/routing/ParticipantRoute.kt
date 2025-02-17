package ru.iuturakulov.mybudgetbackend.routing

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.participant.ParticipantController
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.models.UserRole

fun Route.participantRoute(participantController: ParticipantController) {
    route("participants") {

        get("{projectId}") {
            val projectId = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val participants = participantController.getProjectParticipants(projectId)
            call.respond(ApiResponseState.success(participants, HttpStatusCode.OK))
        }

        post {
            val request = call.receive<AddParticipantRequest>()
            val participant = participantController.addParticipant(request)
            call.respond(ApiResponseState.success(participant, HttpStatusCode.Created))
        }

        delete("{participantId}") {
            val participantId = call.parameters["participantId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val removed = participantController.removeParticipant(participantId)
            if (removed) {
                call.respond(ApiResponseState.success("Участник удален", HttpStatusCode.OK))
            } else {
                call.respond(ApiResponseState.failure("Ошибка удаления", HttpStatusCode.NotFound))
            }
        }

        put("{participantId}/role") {
            val participantId = call.parameters["participantId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val newRole = call.receive<UserRole>()
            val updated = participantController.updateParticipantRole(participantId, newRole)
            if (updated) {
                call.respond(ApiResponseState.success("Роль обновлена", HttpStatusCode.OK))
            } else {
                call.respond(ApiResponseState.failure("Ошибка обновления", HttpStatusCode.NotFound))
            }
        }
    }
}
