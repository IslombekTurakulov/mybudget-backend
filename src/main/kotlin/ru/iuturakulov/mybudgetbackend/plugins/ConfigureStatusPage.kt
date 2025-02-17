package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import ru.iuturakulov.mybudgetbackend.extensions.ApiResponseState.failure
import ru.iuturakulov.mybudgetbackend.extensions.AppException
import java.util.*

private val logger = LoggerFactory.getLogger("StatusPage")

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is ConstraintViolationException ->
                    call.respondError(HttpStatusCode.BadRequest, "Ошибка валидации", cause)

                is MissingRequestParameterException ->
                    call.respondError(HttpStatusCode.BadRequest, "Пропущен параметр: ${cause.parameterName}", cause)

                is AppException.NotFound ->
                    call.respondError(HttpStatusCode.NotFound, cause.message, cause)

                is AppException.Authentication ->
                    call.respondError(HttpStatusCode.Unauthorized, cause.message, cause)

                is AppException.Authorization ->
                    call.respondError(HttpStatusCode.Forbidden, cause.message, cause)

                is AppException.AlreadyExists ->
                    call.respondError(HttpStatusCode.Conflict, cause.message, cause)

                is AppException.InvalidProperty ->
                    call.respondError(HttpStatusCode.BadRequest, cause.message, cause)

                is AppException.RateLimitExceeded ->
                    call.respondError(HttpStatusCode.TooManyRequests, cause.message, cause)

                is AppException.DatabaseError ->
                    call.respondError(HttpStatusCode.InternalServerError, "Ошибка базы данных", cause)

                is AppException.InvalidToken ->
                    call.respondError(HttpStatusCode.Unauthorized, "Неверный или устаревший токен", cause)

                is AppException.ActionNotAllowed ->
                    call.respondError(HttpStatusCode.Forbidden, "Запрещенное действие", cause)

                else -> {
                    logger.error("Неизвестная ошибка", cause)
                    call.respondError(HttpStatusCode.InternalServerError, "Внутренняя ошибка сервера", cause)
                }
            }
        }
    }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String?, cause: Throwable? = null) {
    val response = mutableMapOf(
        "status" to status.value,
        "error" to message
    )

    // Включаем трассировку ошибок только в режиме DEBUG
    if (System.getenv("DEBUG") == "true" && cause != null) {
        response["trace"] = cause.stackTraceToString()
    }

    respond(status, response)
}