package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState.failure
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import java.util.*

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            when (error) {
                is ConstraintViolationException -> {
                    val errorMessage = error.constraintViolations
                        .mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
                        .map { "${it.property}: ${it.message}" }
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure(errorMessage, HttpStatusCode.BadRequest)
                    )
                }

                is MissingRequestParameterException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure("${error.message}", HttpStatusCode.BadRequest)
                    )
                }

                is AppException.AlreadyExists -> {
                    call.respond(
                        HttpStatusCode.Conflict,
                        failure(error.message, HttpStatusCode.Conflict)
                    )
                }

                is AppException.InvalidProperty -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                is AppException.NotFound -> {
                    call.respond(
                        HttpStatusCode.NotFound,
                        failure(error.message, HttpStatusCode.NotFound)
                    )
                }

                is AppException.Authorization -> {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        failure(error.message, HttpStatusCode.Forbidden)
                    )
                }

                is AppException.Authentication -> {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        failure(error.message, HttpStatusCode.Unauthorized)
                    )
                }

                is AppException.Common -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                is NullPointerException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure("Null pointer error: ${error.message}", HttpStatusCode.BadRequest)
                    )
                }

                is TypeCastException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        failure("Type cast exception", HttpStatusCode.BadRequest)
                    )
                }

                else -> {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        failure("Internal server error: ${error.message}", HttpStatusCode.InternalServerError)
                    )
                }
            }
        }

        status(HttpStatusCode.Unauthorized) { call, statusCode ->
            call.respond(
                HttpStatusCode.Unauthorized,
                failure("Unauthorized api call", statusCode)
            )
        }
    }
}