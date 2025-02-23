package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.notification.NotificationController
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Route.notificationRoute(notificationController: NotificationController) {
    route("notifications") {

        get({
            tags("Notifications")
            protected = true
            summary = "Получить список уведомлений пользователя"
            apiResponse()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respondUnauthorized()
            val notifications = notificationController.getUserNotifications(userId)
            call.respond(ApiResponseState.success(notifications, HttpStatusCode.OK))
        }

        put("{notificationId}/read", {
            tags("Notifications")
            protected = true
            summary = "Отметить уведомление как прочитанное"
            request {
                pathParameter<String>("notificationId") { description = "ID уведомления" }
            }
            apiResponse()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@put call.respondUnauthorized()
            val notificationId =
                call.parameters["notificationId"] ?: return@put call.respondBadRequest("ID уведомления обязателен")

            val success = notificationController.markNotificationAsRead(userId, notificationId)
            if (success) {
                call.respond(ApiResponseState.success("Уведомление отмечено как прочитанное", HttpStatusCode.OK))
            } else {
                call.respond(ApiResponseState.failure("Не удалось отметить уведомление", HttpStatusCode.BadRequest))
            }
        }

        delete("{notificationId}", {
            tags("Notifications")
            protected = true
            summary = "Удалить уведомление"
            request {
                pathParameter<String>("notificationId") { description = "ID уведомления" }
            }
            apiResponse()
        }) {
            val userId = call.principal<JwtTokenBody>()?.userId ?: return@delete call.respondUnauthorized()
            val notificationId =
                call.parameters["notificationId"] ?: return@delete call.respondBadRequest("ID уведомления обязателен")

            val success = notificationController.deleteNotification(userId, notificationId)
            if (success) {
                call.respond(ApiResponseState.success("Уведомление удалено", HttpStatusCode.OK))
            } else {
                call.respond(ApiResponseState.failure("Не удалось удалить уведомление", HttpStatusCode.BadRequest))
            }
        }
    }
}
