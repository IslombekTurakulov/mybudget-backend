package ru.iuturakulov.mybudgetbackend.extensions

import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import org.jetbrains.exposed.sql.transactions.transaction
import ru.iuturakulov.mybudgetbackend.models.user.body.JwtTokenBody

object ApiExtensions {

    suspend fun <T> query(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction {
            block()
        }
    }

    fun ApplicationCall.currentUser(): JwtTokenBody {
        return this.principal<JwtTokenBody>() ?: throw IllegalStateException("No authenticated user found")
    }

    suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
        val missingParams = requiredParams.filterNot { this.parameters.contains(it) }
        if (missingParams.isNotEmpty()) {
            this.respond(ApiResponseState.success("Missing parameters: $missingParams", HttpStatusCode.BadRequest))
            return null
        }
        return requiredParams.map { this.parameters[it]!! }
    }

    fun sendEmail(
        toEmail: String,
        verificationCode: String,
        fromEmail: String = SmtpServer.SENDING_EMAIL,
        subject: String = SmtpServer.EMAIL_SUBJECT,
        smtpHost: String = SmtpServer.HOST_NAME,
        smtpPort: Int = SmtpServer.PORT,
        smtpUser: String = SmtpServer.DEFAULT_AUTHENTICATOR,
        smtpPassword: String = SmtpServer.DEFAULT_AUTHENTICATOR_PASSWORD
    ) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                SimpleEmail().apply {
                    hostName = smtpHost
                    setSmtpPort(smtpPort)
                    setAuthenticator(DefaultAuthenticator(smtpUser, smtpPassword))
                    isSSLOnConnect = true
                    setFrom(fromEmail)
                    this.subject = subject
                    setMsg("Your verification code is: $verificationCode")
                    addTo(toEmail)
                    send()
                }
            }
        } catch (e: EmailException) {
            throw AppException.InvalidProperty.Email("Failed to send email: ${e.message}")
        } catch (e: Exception) {
            throw AppException.Common("Email sending failed", e)
        }
    }

    object SmtpServer {
        const val HOST_NAME = "smtp.yandex.ru"
        const val PORT = 465
        const val DEFAULT_AUTHENTICATOR = "yndx-iuturakulov-khevj0@yandex.ru"
        const val DEFAULT_AUTHENTICATOR_PASSWORD = "ajiprghaflyfjgnn"
        const val EMAIL_SUBJECT = "Forget Password"
        const val SENDING_EMAIL = "yndx-iuturakulov-khevj0@yandex.ru"
    }
}