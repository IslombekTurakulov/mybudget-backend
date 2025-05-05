package ru.iuturakulov.mybudgetbackend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.slf4j.event.Level
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

object LoggingPlugin {
    private const val REQUEST_ID_HEADER = "X-Request-ID"
    private const val CORRELATION_ID_HEADER = "X-Correlation-ID"

    fun Application.configureLogging() {
        install(CallId) {
            header(REQUEST_ID_HEADER)
            verify { callId: String ->
                callId.isNotEmpty() || throw IllegalArgumentException("Empty call ID")
            }
            generate {
                UUID.randomUUID().toString()
            }
        }

        install(CallLogging) {
            level = Level.INFO
            callIdMdc(REQUEST_ID_HEADER)
            mdc(CORRELATION_ID_HEADER) { call ->
                call.request.headers[CORRELATION_ID_HEADER] ?: "none"
            }

            format { call ->
                val status = call.response.status() ?: HttpStatusCode.NotFound
                val method = call.request.httpMethod.value
                val path = call.request.path()
                val queryParams = call.request.queryParameters.entries()
                    .joinToString(", ") { (key, values) -> "$key=${values.joinToString(",")}" }
                    .takeIf { it.isNotEmpty() } ?: "none"

                val duration = call.processingTimeMillis().milliseconds
                val remoteHost = call.request.origin.remoteHost
                val userAgent = call.request.headers["User-Agent"] ?: "unknown"
                val language = call.request.headers["X-Language"]
                val contentType = call.request.contentType()?.toString() ?: "none"
                val contentLength = call.request.contentLength() ?: 0
                val requestId = call.callId
                val correlationId = call.request.headers[CORRELATION_ID_HEADER] ?: "none"

                """
                {
                    "timestamp": "${System.currentTimeMillis()}",
                    "requestId": "$requestId",
                    "correlationId": "$correlationId",
                    "status": ${status.value},
                    "method": "$method",
                    "path": "$path",
                    "language": "$language"
                    "queryParams": "$queryParams",
                    "remoteHost": "$remoteHost",
                    "userAgent": "$userAgent",
                    "contentType": "$contentType",
                    "contentLength": $contentLength,
                    "durationMs": ${duration.inWholeMilliseconds},
                    "headers": ${call.request.headers.toMap().toJsonString()}
                }
                """.trimIndent()
            }
        }
    }

    private fun Map<String, List<String>>.toJsonString(): String {
        return entries.joinToString(", ", "{", "}") { (key, values) ->
            "\"$key\": ${values.toJsonValue()}"
        }
    }

    private fun List<String>.toJsonValue(): String {
        return if (size == 1) "\"${first()}\"" else joinToString(", ", "[", "]") { "\"$it\"" }
    }

    private fun PipelineContext<*, ApplicationCall>.logError(throwable: Throwable) {
        val call = call
        val logger = call.application.log
        logger.error(
            """
            Request failed: ${throwable.message}
            Method: ${call.request.httpMethod.value}
            Path: ${call.request.path()}
            Headers: ${call.request.headers.toMap()}
        """.trimIndent(), throwable
        )
    }
}