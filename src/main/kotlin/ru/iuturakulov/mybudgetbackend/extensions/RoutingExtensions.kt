package ru.iuturakulov.mybudgetbackend.extensions

import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*

object RoutingExtensions {

    fun OpenApiRoute.apiResponse() {
        return response {
            HttpStatusCode.OK to {
                description = "Successful"
                body<ApiResponse> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    description = "Successful"
                }
            }
            HttpStatusCode.InternalServerError
        }
    }
}