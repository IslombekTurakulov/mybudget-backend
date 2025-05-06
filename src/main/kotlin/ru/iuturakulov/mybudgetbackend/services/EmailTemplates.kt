package ru.iuturakulov.mybudgetbackend.services

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EmailTemplates {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun registration(name: String, email: String, createdAt: Instant, zone: ZoneId): EmailContent {
        val time = LocalDateTime.ofInstant(createdAt, zone).format(formatter)

        val subject = "Регистрация / Registration"
        val message = """
            Дорогой $name,

            Вы успешно зарегистрировались в системе "Мой бюджет".
            Ваш email: $email
            Время регистрации: $time

            Если вы не совершали регистрацию, пожалуйста, свяжитесь с поддержкой.

            ———

            Dear $name,

            You have successfully registered in the "My Budget" system.
            Your email: $email
            Registration time: $time

            If you did not perform this registration, please contact support.

            Best regards,  
            «My Budget»
        """.trimIndent()

        return EmailContent(subject, message)
    }

    fun passwordReset(email: String, newPassword: String): EmailContent {
        val subject = "Сброс пароля / Password Reset"
        val message = """
            Здравствуйте,

            Вы запросили сброс пароля для вашей учётной записи $email.
            Ваш новый пароль: $newPassword

            Не забудьте сменить его после входа в систему.

            ———

            Hello,

            You have requested a password reset for your account $email.
            Your new password: $newPassword

            Don’t forget to change it after logging in.

            Regards,  
            «My Budget»
        """.trimIndent()

        return EmailContent(subject, message)
    }

    fun passwordChange(
        name: String,
        email: String,
        newPassword: String,
        changedAt: Instant,
        zone: ZoneId
    ): EmailContent {
        val time = LocalDateTime.ofInstant(changedAt, zone).format(formatter)

        val subject = "Смена пароля / Password Change"
        val message = """
            Дорогой $name,

            Пароль был изменён для вашей учётной записи: $email
            Новый пароль: $newPassword
            Время изменения: $time

            Если вы не меняли пароль — срочно свяжитесь с поддержкой.

            ———

            Dear $name,

            Your password has been changed for the account: $email
            New password: $newPassword
            Time of change: $time

            If you did not change the password — please contact support immediately.

            Best regards,  
            «My Budget»
        """.trimIndent()

        return EmailContent(subject, message)
    }

    fun verification(email: String, code: String, forReset: Boolean = false): EmailContent {
        val subject = if (forReset) {
            "Сброс пароля: подтверждение / Password Reset: Verification"
        } else {
            "Подтверждение Email / Email Verification"
        }

        val introRu = if (forReset) {
            "Вы запрашивали сброс пароля. Чтобы продолжить, подтвердите почту."
        } else {
            "Вы запросили подтверждение почты для создания аккаунта в системе «Мой Бюджет»."
        }

        val introEn = if (forReset) {
            "You requested a password reset. To continue, please verify your email."
        } else {
            "You requested email verification to create an account in the \"My Budget\" system."
        }

        val message = """
        Здравствуйте,

        $introRu

        Ваш код подтверждения: $code

        Если вы не запрашивали этот код, просто проигнорируйте это письмо.

        ———

        Hello,

        $introEn

        Your verification code: $code

        If you did not request this code, just ignore this message.

        Regards,  
        «My Budget»
    """.trimIndent()

        return EmailContent(subject, message)
    }
}
