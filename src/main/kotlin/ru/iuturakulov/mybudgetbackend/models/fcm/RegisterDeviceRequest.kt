package ru.iuturakulov.mybudgetbackend.models.fcm

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val language: String,
    val platform: String = "android",
)