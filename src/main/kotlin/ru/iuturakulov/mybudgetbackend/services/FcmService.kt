package ru.iuturakulov.mybudgetbackend.services

import com.google.auth.oauth2.GoogleCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationPayload
import ru.iuturakulov.mybudgetbackend.entities.notification.NotificationType
import java.io.File

class FcmService(
    serviceAccountPath: String,
    private val fcmProjectId: String
) {

    private val logger = LoggerFactory.getLogger("FcmService")

    private val credentials: GoogleCredentials = GoogleCredentials
        .fromStream(File(serviceAccountPath).absoluteFile.inputStream())
        .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    /** Получает и обновляет OAuth2-токен */
    private suspend fun fetchAccessToken(): String = withContext(Dispatchers.IO) {
        credentials.refreshIfExpired()
        credentials.accessToken.tokenValue
    }

    /**
     * Отправляет единичное пуш-уведомление на device token.
     */
    suspend fun send(
        token: String,
        title: String,
        body: String,
        type: NotificationType,
        transactionId: String? = null,
        senderId: String? = null,
        businessProjectId: String? = null, // <-- это ID проекта из бизнес-логики
        extra: Map<String, String> = emptyMap()
    ) {
        try {
            val accessToken = fetchAccessToken()

            val url = "https://fcm.googleapis.com/v1/projects/$fcmProjectId/messages:send"

            // Собираем полезную нагрузку (в виде JSON-строки)
            val payload = NotificationPayload(
                type = type,
                projectId = businessProjectId,
                transactionId = transactionId,
                senderId = senderId,
                customData = extra.takeIf { it.isNotEmpty() }
            )
            val payloadJson = Json.encodeToString(NotificationPayload.serializer(), payload)

            // Формируем плоскую карту data
            val dataMap: Map<String, String> = buildMap {
                put("payload", payloadJson)
                put("type", type.name)
                businessProjectId?.let { put("projectId", it) }
                transactionId?.let { put("transactionId", it) }
                putAll(extra)
            }

            // Готовим тело запроса к FCM
            val request = FcmMessageRequest(
                message = FcmMessage(
                    token = token,
                    notification = FcmNotification(title = title, body = body),
                    data = dataMap
                )
            )

            // Отправка запроса
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                logger.error(errorText) { "FCM send failed with ${response.status.value}: $errorText" }
                throw RuntimeException("FCM send failed: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e.localizedMessage) { "Error sending FCM notification of type $type to token=$token" }
        }
    }



    @kotlinx.serialization.Serializable
    data class FcmNotification(
        val title: String,
        val body: String
    )

    @kotlinx.serialization.Serializable
    data class FcmMessage(
        val token: String,
        val notification: FcmNotification,
        val data: Map<String, String>? = null
    )

    @Serializable
    data class FcmMessageRequest(
        val message: FcmMessage
    )
}