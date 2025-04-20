package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.analytics.AnalyticsController
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsExportFormat
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsExportFrom
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsFilter
import ru.iuturakulov.mybudgetbackend.models.analytics.Granularity
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Route.analyticsRoute(analyticsController: AnalyticsController) {
    authenticate("auth-jwt") {
        route("analytics") {

            get("export", {
                tags("Analytics")
                protected = true
                summary = "Экспорт аналитики пользователя"
                request {
                    queryParameter<AnalyticsExportFormat>("format") { description = "Формат экспорта" }
                    queryParameter<String>("projectId") { description = "ID проекта" }
                    queryParameter<Long>("fromDate") { description = "Начало периода (timestamp)" }
                    queryParameter<Long>("toDate") { description = "Конец периода (timestamp)" }
                    queryParameter<List<String>>("categories") { description = "Фильтр по категориям" }
                    queryParameter<Granularity>("granularity") { description = "Фильтр по периоду" }
                }
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                    val projectId = call.parameters["projectId"]

                    val format = call.request.queryParameters["format"]?.let { AnalyticsExportFormat.safeValueOf(it) }
                        ?: AnalyticsExportFormat.PDF

                    val fromDate = call.request.queryParameters["fromDate"]?.toLongOrNull()
                    val toDate = call.request.queryParameters["toDate"]?.toLongOrNull()

                    val granularity = call.request.queryParameters["granularity"]
                        ?.let { Granularity.safeValueOf(it) } ?: Granularity.MONTH

                    val categories = call.request.queryParameters.getAll("categories") ?: emptyList()

                    val filter = AnalyticsFilter(
                        fromDate = fromDate,
                        toDate = toDate,
                        categories = categories,
                        granularity = granularity
                    )

                    val file = analyticsController.exportAnalytics(
                        userId = userId,
                        projectId = projectId,
                        filter = filter,
                        format = format
                    )

                    call.respondBytes(
                        contentType = file.contentType,
                        status = HttpStatusCode.OK,
                        provider = { file.data }
                    )
                } catch (e: AppException.Authorization) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Вы не можете экспортировать этот проект"
                    )
                } catch (e: AppException.NotFound.Project) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Проект не найден"
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        e.localizedMessage
                    )
                }
            }

            get("overview", {
                tags("Analytics")
                protected = true
                summary = "Получить общую аналитику пользователя"
                request {
                    queryParameter<Long>("fromDate") { description = "Начало периода (timestamp)" }
                    queryParameter<Long>("toDate") { description = "Конец периода (timestamp)" }
                    queryParameter<List<String>>("categories") { description = "Фильтр по категориям" }
                    queryParameter<Granularity>("granularity") { description = "Фильтр по периоду" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()

                val fromDate = call.request.queryParameters["fromDate"]?.toLongOrNull()
                val toDate = call.request.queryParameters["toDate"]?.toLongOrNull()

                val granularity = Granularity.entries.find {
                    it.name.lowercase() == call.request.queryParameters["granularity"]?.lowercase()
                } ?: Granularity.MONTH

                val categories = call.request.queryParameters.getAll("categories") ?: emptyList()

                val filter = AnalyticsFilter(fromDate, toDate, categories, granularity)

                val analytics = analyticsController.getOverviewAnalytics(userId, filter)
                call.respond(HttpStatusCode.OK, analytics)
            }

            get("project/{projectId}", {
                tags("Analytics")
                protected = true
                summary = "Получить аналитику проекта с фильтрацией по датам и категориям"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    queryParameter<Long>("fromDate") { description = "Начало периода (timestamp)" }
                    queryParameter<Long>("toDate") { description = "Конец периода (timestamp)" }
                    queryParameter<List<String>>("categories") { description = "Фильтр по категориям" }
                    queryParameter<Granularity>("granularity") { description = "Фильтр по периоду" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

                val fromDate = call.request.queryParameters["fromDate"]?.toLongOrNull()
                val toDate = call.request.queryParameters["toDate"]?.toLongOrNull()

                val granularity = Granularity.entries.find {
                    it.name.lowercase() == call.request.queryParameters["granularity"]?.lowercase()
                } ?: Granularity.MONTH

                val categories = call.request.queryParameters.getAll("categories") ?: emptyList()

                val filter = AnalyticsFilter(fromDate, toDate, categories, granularity)
                val analytics = analyticsController.getProjectAnalytics(userId, projectId, filter)

                call.respond(HttpStatusCode.OK, analytics)
            }
        }
    }
}