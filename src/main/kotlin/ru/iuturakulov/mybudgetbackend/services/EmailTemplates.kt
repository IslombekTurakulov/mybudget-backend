package ru.iuturakulov.mybudgetbackend.services

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EmailTemplates {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun registration(name: String, email: String, createdAt: Instant, zone: ZoneId): EmailContent {
        val time = LocalDateTime.ofInstant(createdAt, zone).format(formatter)
        val subject = "Вы успешно зарегистрировались!"
        val message = """
            Дорогой $name,

            Вы успешно зарегистрировались в системе "Мой бюджет". 
            Ваш email: $email
            Время регистрации: $time

            Если вы не совершали регистрацию, пожалуйста, свяжитесь с поддержкой.
            """.trimIndent()
        return EmailContent(subject, message)
    }

    fun passwordReset(email: String, newPassword: String): EmailContent {
        val subject = "Восстановление пароля"
        val message = "Ваш новый пароль: $newPassword"
        return EmailContent(subject, message)
    }

    fun passwordChange(name: String, email: String, newPassword: String, changedAt: Instant, zone: ZoneId): EmailContent {
        val time = LocalDateTime.ofInstant(changedAt, zone).format(formatter)
        val subject = "Смена пароля"
        val message = """
            Дорогой $name,

            Пароль изменён для аккаунта: $email
            Новый пароль: $newPassword
            Время смены: $time

            Если вы не совершали это действие, свяжитесь с поддержкой.
            """.trimIndent()
        return EmailContent(subject, message)
    }
}
