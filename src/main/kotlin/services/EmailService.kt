package services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import ru.iuturakulov.mybudgetbackend.extensions.AppException

class EmailService {

    fun sendVerificationEmail(email: String, code: String) {
        sendEmail(email, subject = "Подтверждение почты", message = "Ваш код подтверждения: $code")
    }

    fun sendPasswordResetEmail(email: String, code: String) {
        sendEmail(email, subject = "Восстановление пароля", message = "Ваш код для сброса пароля: $code")
    }

    fun sendEmail(
        toEmail: String,
        subject: String,
        message: String,
        fromEmail: String = SmtpServer.SENDING_EMAIL,
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
                    setMsg(message)
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
        const val SENDING_EMAIL = "yndx-iuturakulov-khevj0@yandex.ru"
    }
}