package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.util.*
import org.slf4j.event.Level

object LoggingPlugin {
    fun Application.configureLogging() {
        install(CallLogging) {
            level = Level.INFO
            format { call ->
                val status = call.response.status() ?: HttpStatusCode.NotFound
                val method = call.request.httpMethod.value
                val path = call.request.path()
                val queryParams = call.request.queryParameters.entries()
                    .joinToString(", ") { entry -> "${entry.key}=${entry.value}" }
                    .ifEmpty { "None" }

                val duration = call.processingTimeMillis()
                val remoteHost = call.request.origin.remoteHost

                """
            |------------------------ Request ------------------------
            |Status: ${colorStatus(status)}
            |Method: $method
            |Path: $path
            |Headers: ${call.request.headers.toMap()}
            |Query Params: $queryParams
            |Remote Host: $remoteHost
            |Duration: ${duration}ms
            |---------------------------------------------------------
            """.trimMargin()
            }
        }
    }

    private fun colorStatus(status: HttpStatusCode): String {
        return when {
            status.value < 300 -> "\u001B[32m$status\u001B[0m" // Green for 2xx
            status.value < 400 -> "\u001B[33m$status\u001B[0m" // Yellow for 3xx
            else -> "\u001B[31m$status\u001B[0m" // Red for 4xx and 5xx
        }
    }
}