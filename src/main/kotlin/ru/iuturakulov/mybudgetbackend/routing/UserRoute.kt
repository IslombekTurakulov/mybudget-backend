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
import ru.iuturakulov.mybudgetbackend.repositories.PreRegistrationEmailRequest
import ru.iuturakulov.mybudgetbackend.repositories.validate

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
            } catch (e: AppException.NotFound.User) {
                call.respond(
                    HttpStatusCode.NotFound,
                    e.message.orEmpty()
                )
            } catch (e: AppException.InvalidProperty.PasswordNotMatch) {
                call.respond(
                    HttpStatusCode.ExpectationFailed,
                    e.message.orEmpty()
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    e.message.orEmpty()
                )
            }
        }

        post("request-register-code", {
            tags("User")
            summary = "Отправка верификационного кода на почту перед регистрацией"
            request {
                body<PreRegistrationEmailRequest>()
            }
            apiResponse()
        }) {
            try {
                val body = call.receive<PreRegistrationEmailRequest>()
                body.validate()
                userController.sendVerificationCode(body.email)

                call.respond(HttpStatusCode.OK, "Код отправлен на почту")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "${e.localizedMessage}")
            }
        }

        post("request-reset-password-code", {
            tags("User")
            summary = "Отправка верификационного кода на почту перед сбросом пароля"
            request {
                body<PreRegistrationEmailRequest>()
            }
            apiResponse()
        }) {
            try {
                val body = call.receive<PreRegistrationEmailRequest>()
                body.validate()
                userController.sendPasswordResetCode(body.email)

                call.respond(HttpStatusCode.OK, "Код отправлен на почту")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "${e.localizedMessage}")
            }
        }

        post("verify-email-registration", {
            tags("User")
            request {
                body<RegistrationRequest>()
            }
            apiResponse()
        }) {
            try {
                val emailRequest = call.receive<RegistrationRequest>()
                emailRequest.validation()
                val result = userController.verifyEmailCode(emailRequest)

                call.respond(HttpStatusCode.OK, result)
            }  catch (e: AppException.AlreadyExists.Email) {
                call.respond(
                    HttpStatusCode.Conflict,
                    "Email уже используется"
                )
            } catch (e: AppException.InvalidProperty.EmailNotExist){
                call.respond(
                    HttpStatusCode.InternalServerError,
                    e.message ?: "Ошибка подтверждения email"
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Ошибка подтверждения email: ${e.localizedMessage.orEmpty()}",
                )
            }
        }

        post("verify-reset-code", {
            tags("User")
            request {
                body<VerifyEmailRequest>()
            }
            apiResponse()
        }) {
            try {
                val emailRequest = call.receive<VerifyEmailRequest>()
                emailRequest.validation()
                val result = userController.verifyPasswordResetCode(
                    email = emailRequest.email,
                    code = emailRequest.code
                )

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Ошибка подтверждения email: ${e.localizedMessage}",
                )
            }
        }

        authenticate("auth-jwt") {
            post("change-password", {
                tags("User")
                protected = true
                request { body<ChangePasswordRequest>() }
                apiResponse()
            }) {
                try {
                    val requestBody = call.receive<ChangePasswordRequest>()
                    requestBody.validation()
                    val userId = call.principal<JwtTokenBody>()?.userId ?: let {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "User with current email is not found"
                        )
                        return@post
                    }
                    val result = userController.changePassword(userId, requestBody)

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
