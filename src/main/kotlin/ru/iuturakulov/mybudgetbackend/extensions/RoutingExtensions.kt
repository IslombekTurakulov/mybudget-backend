package ru.iuturakulov.mybudgetbackend.extensions

import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*
import io.ktor.server.application.*

object RoutingExtensions {

    fun OpenApiRoute.apiResponse() {
        return response {
            HttpStatusCode.OK to {
                description = "Successful"
                body<ApiResponse<Any?>> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    description = "Successful"
                }
            }
            HttpStatusCode.InternalServerError
        }
    }

    /**
     * Отправляет ответ 401 Unauthorized
     */
    suspend fun ApplicationCall.respondUnauthorized() {
        respond(ApiResponseState.failure("Unauthorized", HttpStatusCode.Unauthorized), typeInfo = null)
    }

    /**
     * Отправляет ответ 400 Bad Request с указанным сообщением
     */
    suspend fun ApplicationCall.respondBadRequest(message: String) {
        respond(ApiResponseState.failure(message, HttpStatusCode.BadRequest), typeInfo = null)
    }
}