package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureBasic() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            // serializeNulls()
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call -> createLogMessage(call) }
    }
}

private fun createLogMessage(call: ApplicationCall): String {
    val status = call.response.status() ?: HttpStatusCode.NotFound
    val httpMethod = call.request.httpMethod.value
    val userAgent = call.request.headers["User-Agent"] ?: "Unknown"
    val path = call.request.path()
    val queryParams = call.request.queryParameters.entries()
        .joinToString(", ") { "${it.key}=${it.value}" }
        .ifEmpty { "None" }

    val duration = call.processingTimeMillis()
    val remoteHost = call.request.origin.remoteHost

    val coloredStatus = colorStatus(status)
    val coloredMethod = "\u001B[36m$httpMethod\u001B[0m"

    return """
        |
        |------------------------ Request Details ------------------------
        |Status: $coloredStatus
        |Method: $coloredMethod
        |Path: $path
        |Query Params: $queryParams
        |Remote Host: $remoteHost
        |User Agent: $userAgent
        |Duration: ${duration}ms
        |------------------------------------------------------------------
        |
    """.trimMargin()
}

private fun colorStatus(status: HttpStatusCode): String {
    return when {
        status.value < 300 -> "\u001B[32m$status\u001B[0m" // Green for 2xx
        status.value < 400 -> "\u001B[33m$status\u001B[0m" // Yellow for 3xx
        else -> "\u001B[31m$status\u001B[0m" // Red for 4xx and 5xx
    }
}