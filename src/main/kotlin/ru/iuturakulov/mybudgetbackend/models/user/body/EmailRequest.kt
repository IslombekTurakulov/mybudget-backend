package ru.iuturakulov.mybudgetbackend.models.user.body

data class EmailRequest(val email: String)

fun generate4DigitCode(): String = (1000..9999).random().toString()