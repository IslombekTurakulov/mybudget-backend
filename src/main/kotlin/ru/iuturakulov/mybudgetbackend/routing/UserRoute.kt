package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.requiredParameters
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.respondBadRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.ChangePasswordRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmailRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RefreshTokenRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationRequest
import ru.iuturakulov.mybudgetbackend.models.user.body.VerifyEmailRequest

fun Route.userRoute(userController: UserController) {
    route("auth") {

        post("login", {
            tags("User")
            request { body<LoginRequest>() }
            apiResponse()
        }) {
            try {
                val requestBody = call.receive<LoginRequest>()
                requestBody.validation()
                val response = userController.login(requestBody)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Ошибка при входе: ${e.localizedMessage}"
                )
            }
        }

        post("register", {
            tags("User")
            request { body<RegistrationRequest>() }
            apiResponse()
        }) {
            try {
                val requestBody = call.receive<RegistrationRequest>()
                requestBody.validation()
                val user = userController.register(requestBody)
                call.respond(HttpStatusCode.Created, user)
            } catch (e: AppException.AlreadyExists.Email) {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Email уже используется"
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Ошибка при регистрации: ${e.localizedMessage}",
                )
            }
        }

        post("verify-email", {
            tags("User")
            request {
                queryParameter<String>("email") { required = true }
                queryParameter<String>("verificationCode") { required = true }
            }
            apiResponse()
        }) {
            try {
                val (email, verificationCode) = call.requiredParameters("email", "verificationCode")
                    ?: return@post call.respondBadRequest("Required params are invalid")
                val emailRequest = VerifyEmailRequest(email, verificationCode)
                emailRequest.validation()
                val result = userController.verifyEmail(emailRequest)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, "Email подтвержден")
                } else {
                    call.respond(ApiResponseState.failure("Неверный код", HttpStatusCode.BadRequest))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Ошибка подтверждения email: ${e.localizedMessage}",
                )
            }
        }

        post("reset-password", {
            tags("User")
            request { body<ForgetPasswordEmailRequest>() }
            apiResponse()
        }) {
            try {
                val requestBody = call.receive<ForgetPasswordEmailRequest>()
                requestBody.validation()
                val verificationCode = userController.requestPasswordReset(requestBody)

                call.respond(verificationCode)
            } catch (e: AppException.InvalidProperty.EmailNotExist) {
                call.respond(HttpStatusCode.NotFound, "Email не найден")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при сбросе пароля")
            }
        }

        put("change-password", {
            tags("User")
            protected = true
            request { body<ChangePasswordRequest>() }
            apiResponse()
        }) {
            try {
                val requestBody = call.receive<ChangePasswordRequest>()
                requestBody.validation()
                val loginUser = call.principal<JwtTokenBody>()?.userId ?: let {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "User with current email is not found"
                    )
                    return@put
                }
                val result = userController.changePassword(loginUser, requestBody)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, "Пароль изменен")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Старый пароль неверен")
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Ошибка изменения пароля: ${e.localizedMessage}"
                )
            }
        }

        post("refresh-token", {
            tags("User")
            request { body<RefreshTokenRequest>() }
            apiResponse()
        }) {
            try {
                val requestBody = call.receive<RefreshTokenRequest>()
                val newAccessToken = userController.refreshToken(requestBody)

                call.respond(HttpStatusCode.OK, newAccessToken)
            } catch (e: AppException.Authentication) {
                call.respond(HttpStatusCode.Unauthorized, "Недействительный refresh-токен")
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Ошибка обновления токена: ${e.localizedMessage}"
                )
            }
        }
    }
}
