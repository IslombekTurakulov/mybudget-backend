package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.project.ProjectController
import ru.iuturakulov.mybudgetbackend.entities.projects.ProjectEntity
import ru.iuturakulov.mybudgetbackend.extensions.AccessControl
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody
import kotlin.text.get

fun Route.projectRoute(projectController: ProjectController, accessControl: AccessControl) {
    route("projects") {

        /**
         * Получение всех проектов, в которых участвует пользователь (OWNER, EDITOR, VIEWER)
         */
        get({
            tags("Projects")
            protected = true
            summary = "Получить список проектов, в которых участвует текущий пользователь"
            apiResponse<List<ProjectEntity>>()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
            val projects = projectController.getProject(userId)
            call.respond(ApiResponseState.success(projects, HttpStatusCode.OK))
        }

        /**
         * Получение конкретного проекта (только для участников)
         */
        get("{projectId}", {
            tags("Projects")
            protected = true
            summary = "Получить проект по ID (только для участников)"
            request { pathParameter<String>("projectId") { required = true } }
            apiResponse<ProjectEntity>()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
            val projectId = call.parameters["projectId"] ?: return@get call.respondBadRequest("Project ID is required")

            val project = projectController.getProjectById(projectId) ?: return@get call.respondNotFound("Проект не найден")
            val participant = projectController.getParticipant(userId, projectId)

            if (!accessControl.canViewProject(userId, project, participant)) {
                return@get call.respondForbidden("У вас нет доступа к этому проекту")
            }

            call.respond(ApiResponseState.success(project, HttpStatusCode.OK))
        }

        /**
         * Создание проекта (владелец становится OWNER)
         */
        post({
            tags("Projects")
            protected = true
            summary = "Создать новый проект"
            request { body<CreateProjectRequest>() }
            apiResponse<ProjectEntity>()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
            val requestBody = call.receive<CreateProjectRequest>()
            requestBody.validation()

            val project = projectController.createProject(userId, requestBody)
            call.respond(ApiResponseState.success(project, HttpStatusCode.Created))
        }

        /**
         * Обновление проекта (только для владельца и редактора)
         */
        put("{projectId}", {
            tags("Projects")
            protected = true
            summary = "Обновить проект (доступно только владельцу и редакторам)"
            request {
                pathParameter<String>("projectId") { required = true }
                body<UpdateProjectRequest>()
            }
            apiResponse<ProjectEntity>()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@put call.respondUnauthorized()
            val projectId = call.parameters["projectId"] ?: return@put call.respondBadRequest("Project ID is required")
            val requestBody = call.receive<UpdateProjectRequest>()

            val project = projectController.getProjectById(projectId) ?: return@put call.respondNotFound("Проект не найден")
            val participant = projectController.getParticipant(userId, projectId)

            if (!accessControl.canEditProject(userId, project, participant)) {
                return@put call.respondForbidden("У вас нет прав на редактирование этого проекта")
            }

            val updatedProject = projectController.updateProject(projectId, requestBody)
            call.respond(ApiResponseState.success(updatedProject, HttpStatusCode.OK))
        }

        /**
         * Удаление проекта (только для владельца)
         */
        delete("{projectId}", {
            tags("Projects")
            protected = true
            summary = "Удалить проект (доступно только владельцу)"
            request { pathParameter<String>("projectId") { required = true } }
            apiResponse<Unit>()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@delete call.respondUnauthorized()
            val projectId = call.parameters["projectId"] ?: return@delete call.respondBadRequest("Project ID is required")

            val project = projectController.getProjectById(projectId) ?: return@delete call.respondNotFound("Проект не найден")

            if (!accessControl.canDeleteProject(userId, project)) {
                return@delete call.respondForbidden("Только владелец может удалить этот проект")
            }

            projectController.deleteProject(projectId)
            call.respond(ApiResponseState.success("Проект успешно удален", HttpStatusCode.OK))
        }
    }
}
