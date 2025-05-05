package ru.iuturakulov.mybudgetbackend.models.fcm

data class DeviceTokenWithLanguageCode(
    val token: String,
    val languageCode: String,
    val userId: String
)
