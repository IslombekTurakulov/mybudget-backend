package ru.iuturakulov.mybudgetbackend.routing

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.iuturakulov.mybudgetbackend.controller.user.UserController
import ru.iuturakulov.mybudgetbackend.entities.user.ChangePassword
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.requiredParameters
import ru.iuturakulov.mybudgetbackend.extensions.ApiExtensions.sendEmail
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState
import ru.iuturakulov.mybudgetbackend.extensions.DataBaseTransaction
import ru.iuturakulov.mybudgetbackend.extensions.RoutingExtensions.apiResponse
import ru.iuturakulov.mybudgetbackend.models.user.body.ConfirmPassword
import ru.iuturakulov.mybudgetbackend.models.user.body.ForgetPasswordEmail
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody
import ru.iuturakulov.mybudgetbackend.models.user.body.LoginBody
import ru.iuturakulov.mybudgetbackend.models.user.body.RegistrationBody

fun Route.userRoute(userController: UserController) {
    route("auth") {
        post("login", {
            tags("User")
            request {
                body<LoginBody>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<LoginBody>()
            call.respond(
                ApiResponseState.success(
                    userController.login(requestBody), HttpStatusCode.OK
                )
            )
        }
        post("register", {
            tags("User")
            request {
                body<RegistrationBody>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<RegistrationBody>()
            requestBody.validation()
            call.respond(ApiResponseState.success(userController.addUser(requestBody), HttpStatusCode.OK))
        }
        get("reset-password", {
            tags("User")
            request {
                queryParameter<String>("email") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email) = call.requiredParameters("email") ?: return@get
            val requestBody = ForgetPasswordEmail(email)
            userController.forgetPasswordSendCode(requestBody).let {
                sendEmail(requestBody.email, it.verificationCode)
                call.respond(

                    ApiResponseState.success(
                        "Verification code sent to ${requestBody.email}",
                        HttpStatusCode.OK
                    )
                )
            }
        }
        get("verify-password-change", {
            tags("User")
            request {
                queryParameter<String>("email") {
                    required = true
                }
                queryParameter<String>("verificationCode") {
                    required = true
                }
                queryParameter<String>("newPassword") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email, verificationCode, newPassword) = call.requiredParameters(
                "email", "verificationCode", "newPassword"
            ) ?: return@get

            UserController().forgetPasswordVerificationCode(
                ConfirmPassword(
                    email, verificationCode, newPassword
                )
            ).let {
                when (it) {
                    DataBaseTransaction.FOUND -> {
                        call.respond(
                            ApiResponseState.success(
                                "Password change successful", HttpStatusCode.OK
                            )
                        )
                    }

                    DataBaseTransaction.NOT_FOUND -> {
                        call.respond(
                            ApiResponseState.success(
                                "Verification code is not valid",
                                HttpStatusCode.OK
                            )
                        )
                    }
                }
            }
        }
            put("change-password", {
                tags("User")
                protected = true
                request {
                    queryParameter<String>("oldPassword") {
                        required = true
                    }
                    queryParameter<String>("newPassword") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (oldPassword, newPassword) = call.requiredParameters("oldPassword", "newPassword") ?: return@put
                val loginUser = call.principal<JwtTokenBody>()
                userController.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword)).let {
                    if (it) call.respond(
                        ApiResponseState.success(
                            "Password has been changed", HttpStatusCode.OK
                        )
                    ) else call.respond(
                        ApiResponseState.failure(
                            "Old password is wrong", HttpStatusCode.OK
                        )
                    )
                }
            }
    }
}