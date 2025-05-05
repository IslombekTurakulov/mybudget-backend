package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondUnauthorized
import ru.iuturakulov.mybudgetbackend.models.fcm.RegisterDeviceRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody
import ru.iuturakulov.mybudgetbackend.repositories.DeviceTokenRepository

fun Route.deviceTokensRoute(deviceTokenRepository: DeviceTokenRepository) {
    authenticate("auth-jwt") {
        route("devices") {
            post("register", {
                tags("Device Token")
                summary = "Зарегистрировать устройство"
                request { body<RegisterDeviceRequest>() }
                apiResponse()
            }) {
                val userId = call.principal<JwtTokenBody>()?.userId ?: return@post call.respondUnauthorized()
                val requestBody = call.receive<RegisterDeviceRequest>()

                try {
                    deviceTokenRepository.registerOrUpdate(
                        userId = userId,
                        token = requestBody.token,
                        platform = requestBody.platform,
                        language = requestBody.language
                    )
                    call.respond(HttpStatusCode.OK, "Устройство зарегистрировано")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.localizedMessage)
                }
            }
        }
    }
}