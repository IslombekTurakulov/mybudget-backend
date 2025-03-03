package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.analytics.AnalyticsController
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsFilter
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Route.analyticsRoute(analyticsController: AnalyticsController) {
    route("analytics") {

        get("overview", {
            tags("Analytics")
            protected = true
            summary = "Получить общую аналитику пользователя"
            apiResponse()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
            val analytics = analyticsController.getOverviewAnalytics(userId)
            call.respond(ApiResponseState.success(analytics, HttpStatusCode.OK))
        }

        get("{projectId}/analytics", {
            tags("Analytics")
            protected = true
            summary = "Получить аналитику проекта с фильтрацией по датам и категориям"
            request {
                pathParameter<String>("projectId") { description = "ID проекта" }
                queryParameter<Long>("fromDate") { description = "Начало периода (timestamp)" }
                queryParameter<Long>("toDate") { description = "Конец периода (timestamp)" }
                queryParameter<List<String>>("categories") { description = "Фильтр по категориям" }
            }
            apiResponse()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
            val projectId = call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

            val fromDate = call.request.queryParameters["fromDate"]?.toLongOrNull()
            val toDate = call.request.queryParameters["toDate"]?.toLongOrNull()
            val categories = call.request.queryParameters.getAll("categories") ?: emptyList()

            val filter = AnalyticsFilter(fromDate, toDate, categories)
            val analytics = analyticsController.getProjectAnalytics(userId, projectId, filter)

            call.respond(ApiResponseState.success(analytics, HttpStatusCode.OK))
        }
    }
}