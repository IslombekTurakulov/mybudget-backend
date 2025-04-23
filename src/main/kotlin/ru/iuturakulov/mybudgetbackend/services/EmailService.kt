package ru.iuturakulov.mybudgetbackend.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import ru.iuturakulov.mybudgetbackend.extensions.AppException

import org.apache.commons.mail.*
import ru.iuturakulov.mybudgetbackend.models.analytics.AnalyticsExportFormat
import javax.activation.*
import javax.mail.util.ByteArrayDataSource

class EmailService {

    /** Отправка простого письма без вложений */
    fun sendEmail(
        toEmail: String,
        subject: String,
        message: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            buildEmail<SimpleEmail>().apply {
                setSubject(subject)
                setMsg(message)
                addTo(toEmail)
                send()
            }
        } catch (e: EmailException) {
            throw AppException.InvalidProperty.Email("Failed to send email: ${e.message}")
        }
    }

    /** Универсальный метод с одним вложением */
    fun sendEmailWithAttachment(
        toEmail: String,
        subject: String,
        message: String,
        attachmentName: String,
        attachmentBytes: ByteArray,
        attachmentMime: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val dataSource: DataSource = ByteArrayDataSource(attachmentBytes, attachmentMime)

            buildEmail<MultiPartEmail>().apply {
                setSubject(subject)
                setMsg(message)
                addTo(toEmail)

                attach(dataSource, attachmentName, "auto‑generated analytics export")

                send()
            }
        } catch (e: EmailException) {
            throw AppException.InvalidProperty.Email("Failed to send email with attachment: ${e.message}")
        }
    }

    /** Удобный обёртка именно для экспорта аналитики */
    fun sendAnalyticsExportEmail(
        topicName: String,
        toEmail: String,
        attachmentName: String,
        attachmentBytes: ByteArray,
        exportFormat: AnalyticsExportFormat   // enum { CSV, PDF }
    ) {
        val mime = when (exportFormat) {
            AnalyticsExportFormat.CSV -> "text/csv"
            AnalyticsExportFormat.PDF -> "application/pdf"
        }

        val message = """
        Здравствуйте!
        
        Во вложении вы найдёте отчёт по аналитике «$topicName» в формате ${exportFormat.name.lowercase()}.
        Надеемся, что данные будут полезны для вас.
        
        Если возникнут вопросы — пишите нам в поддержку.
        
        С уважением,
        «Мой Бюджет»
        """.trimIndent()

        sendEmailWithAttachment(
            toEmail = toEmail,
            subject = "Экспорт аналитики «$topicName»",
            message = message,
            attachmentName = attachmentName,
            attachmentBytes = attachmentBytes,
            attachmentMime = mime
        )
    }


    /**
     * Единообразно настраиваем SMTP‑параметры
     */
    private inline fun <reified T : Email> buildEmail(): T {
        val email = when (T::class) {
            SimpleEmail::class -> SimpleEmail()
            MultiPartEmail::class -> MultiPartEmail()
            else -> error("Unsupported email type")
        } as T

        with(email) {
            hostName = SmtpServer.HOST_NAME
            setSmtpPort(SmtpServer.PORT)
            setAuthenticator(
                DefaultAuthenticator(
                    SmtpServer.DEFAULT_AUTHENTICATOR,
                    SmtpServer.DEFAULT_AUTHENTICATOR_PASSWORD
                )
            )
            isSSLOnConnect = true
            setFrom(SmtpServer.SENDING_EMAIL)
            setCharset("UTF-8")
        }
        return email
    }

    object SmtpServer {
        const val HOST_NAME = "smtp.yandex.ru"
        const val PORT = 465
        const val DEFAULT_AUTHENTICATOR = "yndx-iuturakulov-khevj0@yandex.ru"
        const val DEFAULT_AUTHENTICATOR_PASSWORD = "ajiprghaflyfjgnn"
        const val SENDING_EMAIL = "yndx-iuturakulov-khevj0@yandex.ru"
    }
}
