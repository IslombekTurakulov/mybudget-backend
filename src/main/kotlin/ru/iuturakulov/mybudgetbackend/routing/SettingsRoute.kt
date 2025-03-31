package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.settings.SettingsController
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.models.settings.UserSettingsRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

fun Route.settingsRoute(settingsController: SettingsController) {
    authenticate("auth-jwt") {
        route("settings") {

            get({
                tags("Settings")
                protected = true
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        "Unauthorized"
                    )

                    val settings = settingsController.getUserSettings(userId)
                    call.respond(HttpStatusCode.OK, settings)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Ошибка получения настроек: ${e.localizedMessage}"
                    )
                }
            }

            put({
                tags("Settings")
                protected = true
                request { body<UserSettingsRequest>() }
                apiResponse()
            }) {
                try {
                    val userId = call.principal<JwtTokenBody>()?.userId ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        "Unauthorized"
                    )

                    val requestBody = call.receive<UserSettingsRequest>()
                    requestBody.validation()

                    val updatedSettings = settingsController.updateUserSettings(userId, requestBody)
                    call.respond(HttpStatusCode.OK, updatedSettings)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Ошибка обновления настроек: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
