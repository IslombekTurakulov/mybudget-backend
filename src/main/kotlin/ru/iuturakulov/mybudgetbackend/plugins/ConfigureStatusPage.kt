package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState.failure
import ru.iuturakulov.mybudgetbackend.extensions.CommonException
import ru.iuturakulov.mybudgetbackend.extensions.EmailNotExistException
import ru.iuturakulov.mybudgetbackend.extensions.PasswordNotMatchException
import ru.iuturakulov.mybudgetbackend.extensions.UserNotFoundException
import java.util.*

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            when (error) {
                is ConstraintViolationException -> {
                    val errorMessage =
                        error.constraintViolations.mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
                            .map { "${it.property}: ${it.message}" }
                    call.respond(
                        HttpStatusCode.BadRequest, failure(
                            errorMessage, HttpStatusCode.BadRequest
                        )
                    )
                }

                is MissingRequestParameterException -> {
                    call.respond(
                        HttpStatusCode.BadRequest, failure(
                            "${error.message}", HttpStatusCode.BadRequest
                        )
                    )
                }

                is EmailNotExistException -> {
                    call.respond(
                        HttpStatusCode.BadRequest, failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                is NullPointerException -> {
                    call.respond(
                        failure(
                            "Null pointer error : ${error.message}", HttpStatusCode.BadRequest
                        )
                    )
                }

                is UserNotFoundException -> {
                    call.respond(
                        HttpStatusCode.BadRequest, failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                is PasswordNotMatchException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                is TypeCastException -> {
                    call.respond(
                        failure("Type cast exception", HttpStatusCode.BadRequest)
                    )
                }

                is CommonException -> {
                    call.respond(
                        HttpStatusCode.BadRequest, failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                else -> {
                    call.respond(
                        HttpStatusCode.InternalServerError, failure(
                            "Internal server error : ${error.message}", HttpStatusCode.InternalServerError
                        )
                    )
                }
            }
        }
        status(HttpStatusCode.Unauthorized) { call, statusCode ->
            call.respond(HttpStatusCode.Unauthorized, failure("Unauthorized api call", statusCode))
        }
    }
}