package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.project.ProjectController
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
import ru.iuturakulov.mybudgetbackend.models.project.AcceptInviteRequest
import ru.iuturakulov.mybudgetbackend.models.project.ChangeRoleRequest
import ru.iuturakulov.mybudgetbackend.models.project.CreateProjectRequest
import ru.iuturakulov.mybudgetbackend.models.project.InviteParticipantRequest
import ru.iuturakulov.mybudgetbackend.models.project.UpdateProjectRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Route.projectRoute(projectController: ProjectController, auditLogService: AuditLogService) {
    authenticate("auth-jwt") {
        route("projects") {
            get({
                tags("Projects")
                protected = true
                summary = "Получить список проектов, в которых участвует пользователь"
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                val projects = projectController.getUserProjects(userId)
                call.respond(HttpStatusCode.OK, projects)
            }

            post("accept-invite", {
                tags("Projects")
                protected = true
                summary = "Принять приглашение в проект"
                request { body<AcceptInviteRequest>() }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                val requestBody = call.receive<AcceptInviteRequest>()

                projectController.acceptInvitation(userId, requestBody).let { result ->
                    if (result) {
                        auditLogService.logAction(userId, "Accepted invite to project: ${requestBody.projectId}")
                        call.respond(HttpStatusCode.OK, "Приглашение принято")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Ошибка принятия приглашения")
                    }
                }
            }

            post("{projectId}/invite", {
                tags("Projects")
                protected = true
                summary = "Пригласить участника в проект"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    body<InviteParticipantRequest>()
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@post call.respondBadRequest("Project ID is required")
                val requestBody = call.receive<InviteParticipantRequest>()

                projectController.inviteParticipant(userId, projectId, requestBody).let { result ->
                    if (result.success) {
                        auditLogService.logAction(userId, "Invited ${requestBody.email} to project: $projectId")
                        call.respond(HttpStatusCode.OK, "Приглашение отправлено")
                    } else {
                        call.respond(HttpStatusCode.TooManyRequests, result.message)
                    }
                }
            }


            get("{projectId}", {
                tags("Projects")
                protected = true
                summary = "Получить информацию о проекте"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

                val project = projectController.getProjectById(userId, projectId)
                call.respond(HttpStatusCode.OK, project)
            }

            post({
                tags("Projects")
                summary = "Создать новый проект"
                request { body<CreateProjectRequest>() }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                val requestBody = call.receive<CreateProjectRequest>()
                val project = projectController.createProject(userId, requestBody)
                call.respond(HttpStatusCode.Created, project)
            }

            put("{projectId}", {
                tags("Projects")
                protected = true
                summary = "Обновить проект"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    body<UpdateProjectRequest>()
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@put call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@put call.respondBadRequest("Project ID is required")
                val requestBody = call.receive<UpdateProjectRequest>()

                val updatedProject = projectController.updateProject(userId, projectId, requestBody)
                call.respond(HttpStatusCode.OK, updatedProject)
            }

            delete("{projectId}", {
                tags("Projects")
                protected = true
                summary = "Удалить проект (только для владельца)"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@delete call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@delete call.respondBadRequest("Project ID is required")

                try {
                    projectController.deleteProject(userId, projectId)
                    auditLogService.logAction(userId, "Удалил проект: $projectId")
                    call.respond(HttpStatusCode.OK, "Проект успешно удален")
                } catch (e: AppException.Authorization) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Вы не можете удалить этот проект"
                    )
                } catch (e: AppException.NotFound.Project) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Проект не найден"
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Add names to call arguments"
                    )
                }
            }

            post("{projectId}/invite", {
                tags("Projects")
                protected = true
                summary = "Пригласить участника в проект"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    body<InviteParticipantRequest>()
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@post call.respondBadRequest("Project ID is required")
                val requestBody = call.receive<InviteParticipantRequest>()

                projectController.inviteParticipant(userId, projectId, requestBody)
                call.respond(ApiResponseState.success("Приглашение отправлено", HttpStatusCode.OK))
            }

            put("{projectId}/role", {
                tags("Projects")
                protected = true
                summary = "Изменить роль участника в проекте"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                    body<ChangeRoleRequest>()
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@put call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@put call.respondBadRequest("Project ID is required")
                val requestBody = call.receive<ChangeRoleRequest>()

                projectController.changeParticipantRole(userId, projectId, requestBody)
                call.respond(ApiResponseState.success("Роль участника изменена", HttpStatusCode.OK))
            }
        }
    }
}
