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
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectStatus
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.AuditLogService
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
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

            post("accept-invite/{inviteCode}", {
                tags("Projects")
                protected = true
                summary = "Принять приглашение в проект"
                request { pathParameter<String>("inviteCode") { description = "Код приглашения" } }
                apiResponse()
            }) {
               try {
                   val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                   val inviteCode =
                       call.parameters["inviteCode"] ?: return@post call.respondBadRequest("Invitation code is required")

                   projectController.acceptInvitation(userId, inviteCode).let { result ->
                       if (result) {
                           call.respond(HttpStatusCode.OK, "Приглашение принято")
                       } else {
                           call.respond(HttpStatusCode.BadRequest, "Ошибка принятия приглашения")
                       }
                   }
               } catch (e: AppException.AlreadyExists.User) {
                   call.respond(HttpStatusCode.Accepted, e.localizedMessage)
               } catch (e: Exception) {
                   call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
               }
            }

            delete( "{projectId}/participants/{participantId}", {
                tags("Projects")
                protected = true
                summary = "Удалить участника из проекта"
                request {
                    pathParameter<String>("participantId") { description = "ID пользователя" }
                    pathParameter<String>("projectId") { description = "ID проекта" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@delete call.respondUnauthorized()
                val participantId =
                    call.parameters["participantId"] ?: return@delete call.respondBadRequest("User ID is required")
                val projectId =
                    call.parameters["projectId"] ?: return@delete call.respondBadRequest("Project ID is required")

                projectController.removeParticipant(userId, projectId, participantId)
                auditLogService.logAction(userId, "Removed $participantId from project: $projectId")
                call.respond(HttpStatusCode.OK, "Пользователь удален из проекта")
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

                val result = projectController.inviteParticipant(userId, projectId, requestBody)
                if (result.success) {
                    auditLogService.logAction(userId, "Invited ${requestBody.email} to project: $projectId")
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.Forbidden, result.message)
                }
            }

            get("{projectId}/participants", {
                tags("Projects")
                protected = true
                summary = "Пригласить участника в проект"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                val projectId = call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

                val participants = projectController.getProjectParticipants(userId, projectId)
                call.respond(HttpStatusCode.OK, participants)
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

                try {
                    requestBody.validation()
                    val updatedProject = if (requestBody.status == ProjectStatus.ARCHIVED) {
                        projectController.archiveProject(userId, projectId, requestBody)
                    } else {
                        projectController.updateProject(userId, projectId, requestBody)
                    }
                    call.respond(HttpStatusCode.OK, updatedProject)
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
                        e.localizedMessage
                    )
                }
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

                try {
                    projectController.changeParticipantRole(userId, projectId, requestBody)
                    call.respond(HttpStatusCode.OK, "Роль участника изменена")
                } catch (e: AppException.Authorization) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        e.localizedMessage
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

            get("{projectId}/currentRole", {
                tags("Projects")
                protected = true
                summary = "Изменить роль участника в проекте"
                request {
                    pathParameter<String>("projectId") { description = "ID проекта" }
                }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
                val projectId =
                    call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

                try {
                    call.respond(HttpStatusCode.OK, projectController.getCurrentUserProjectRole(userId, projectId))
                } catch (e: AppException.Authorization) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        e.localizedMessage
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
        }
    }
}
