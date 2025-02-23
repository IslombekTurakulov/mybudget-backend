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

                call.respond(ApiResponseState.success(response, HttpStatusCode.OK))
            } catch (e: Exception) {
                call.respond(
                    ApiResponseState.failure(
                        "Ошибка при входе: ${e.localizedMessage}",
                        HttpStatusCode.BadRequest
                    )
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
                call.respond(ApiResponseState.success(user, HttpStatusCode.Created))
            } catch (e: AppException.AlreadyExists.Email) {
                call.respond(ApiResponseState.failure("Email уже используется", HttpStatusCode.Conflict))
            } catch (e: Exception) {
                call.respond(
                    ApiResponseState.failure(
                        "Ошибка при регистрации: ${e.localizedMessage}",
                        HttpStatusCode.BadRequest
                    )
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
                    call.respond(ApiResponseState.success("Email подтвержден", HttpStatusCode.OK))
                } else {
                    call.respond(ApiResponseState.failure("Неверный код", HttpStatusCode.BadRequest))
                }
            } catch (e: Exception) {
                call.respond(
                    ApiResponseState.failure(
                        "Ошибка подтверждения email: ${e.localizedMessage}",
                        HttpStatusCode.InternalServerError
                    )
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
                call.respond(ApiResponseState.failure("Email не найден", HttpStatusCode.NotFound))
            } catch (e: Exception) {
                call.respond(ApiResponseState.failure("Ошибка при сбросе пароля", HttpStatusCode.InternalServerError))
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
                        ApiResponseState.failure(
                            error = "User with current email is not found",
                            statsCode = HttpStatusCode.InternalServerError
                        )
                    )
                    return@put
                }
                val result = userController.changePassword(loginUser, requestBody)

                if (result.isSuccess) {
                    call.respond(ApiResponseState.success("Пароль изменен", HttpStatusCode.OK))
                } else {
                    call.respond(ApiResponseState.failure("Старый пароль неверен", HttpStatusCode.BadRequest))
                }
            } catch (e: Exception) {
                call.respond(
                    ApiResponseState.failure(
                        "Ошибка изменения пароля: ${e.localizedMessage}",
                        HttpStatusCode.InternalServerError
                    )
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

                call.respond(ApiResponseState.success(newAccessToken, HttpStatusCode.OK))
            } catch (e: AppException.Authentication) {
                call.respond(ApiResponseState.failure("Недействительный refresh-токен", HttpStatusCode.Unauthorized))
            } catch (e: Exception) {
                call.respond(
                    ApiResponseState.failure(
                        "Ошибка обновления токена: ${e.localizedMessage}",
                        HttpStatusCode.InternalServerError
                    )
                )
            }
        }
    }
}
